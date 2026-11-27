import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { authQueryKeys, useCurrentUserQuery } from "@/features/auth";
import { marketQueryKeys } from "@/features/market/constants";
import { useBuyQuantityForm } from "@/features/market/form";
import { useBuyTokensMutation } from "@/features/market/hooks/mutations";
import { useMarketToast } from "@/features/market/hooks/useMarketToast";
import { marketService } from "@/features/market/market.service";
import type {
	BuyEstimation,
	BuyResult,
	OfficialMarketPlayer,
	OfficialMarketQuote,
} from "@/features/market/types";
import { calculateEstimation } from "@/features/market/utils";
import { playersQueryKeys } from "@/features/players/constants";
import { portfolioQueryKeys } from "@/features/portfolio";
import { getErrorCode } from "@/infrastructure/http";
import { AuthStatus, useAuthStore } from "@/infrastructure/storage";

function mapErrorCodeToMessage(error: unknown): string {
	switch (getErrorCode(error)) {
		case "INVALID_REQUEST":
		case "INVALID_QUANTITY":
			return "La cantidad ingresada no es válida.";
		case "INSUFFICIENT_CREDITS":
			return "Créditos insuficientes para completar la compra.";
		case "INSUFFICIENT_STOCK":
			return "No hay tokens disponibles para este jugador.";
		case "QUOTE_UNAVAILABLE":
		case "INVALID_QUOTE_PRICE":
			return "La cotización dejó de estar disponible.";
		case "SUPERUSER_CANNOT_BUY":
			return "Los administradores no pueden comprar tokens.";
		case "PLAYER_NOT_FOUND":
		case "PLAYER_NOT_VISIBLE":
			return "El jugador ya no está disponible.";
		default:
			return "No se pudo completar la compra. Intentá nuevamente.";
	}
}

/**
 * Hook para gestionar la apertura, cotización, cotización reactiva y compra de tokens
 * desde las tablas de tendencias del mercado.
 */
export function useMarketTrendsBuyModal() {
	const queryClient = useQueryClient();
	const { notifySuccess, notifyError } = useMarketToast();

	const [selectedPlayer, setSelectedPlayer] =
		useState<OfficialMarketPlayer | null>(null);
	const [purchaseResult, setPurchaseResult] = useState<BuyResult | null>(null);
	const [errorMessage, setErrorMessage] = useState<string | null>(null);
	const [isSuccess, setIsSuccess] = useState(false);

	const isSubmittingRef = useRef(false);
	const triggerRef = useRef<HTMLElement | null>(null);
	const initialFocusRef = useRef<HTMLInputElement | null>(null);
	const autoCloseTimeoutRef = useRef<number | null>(null);

	const revision = useAuthStore((state) => state.revision);
	const authenticated = useAuthStore(
		(state) =>
			state.status === AuthStatus.AUTHENTICATED && state.session !== null,
	);
	const currentUser = useCurrentUserQuery(authenticated, revision);
	const isSuperuser = currentUser.data?.role === "SUPERUSER";

	const form = useBuyQuantityForm();
	const buyMutation = useBuyTokensMutation();

	// Limpiar temporizador al desmontar
	useEffect(() => {
		return () => {
			if (autoCloseTimeoutRef.current !== null) {
				window.clearTimeout(autoCloseTimeoutRef.current);
			}
		};
	}, []);

	const closeBuyModal = useCallback(() => {
		if (buyMutation.isPending || isSubmittingRef.current) return;
		if (autoCloseTimeoutRef.current !== null) {
			window.clearTimeout(autoCloseTimeoutRef.current);
			autoCloseTimeoutRef.current = null;
		}
		setSelectedPlayer(null);
		setPurchaseResult(null);
		setErrorMessage(null);
		setIsSuccess(false);
		form.resetQuantity();
		if (triggerRef.current) {
			triggerRef.current.focus();
		}
	}, [buyMutation.isPending, form]);

	const openBuyModal = useCallback(
		async (player: OfficialMarketPlayer, triggerEl?: HTMLElement | null) => {
			if (triggerEl) {
				triggerRef.current = triggerEl;
			}
			setPurchaseResult(null);
			setErrorMessage(null);
			setIsSuccess(false);
			isSubmittingRef.current = false;
			form.resetQuantity();

			let playerToSet = player;

			// Si el jugador no viene con cotización, consultamos de forma asíncrona
			if (!playerToSet.quote) {
				try {
					const quoteData = await marketService.getCurrentQuote(player.id);
					if (quoteData) {
						playerToSet = {
							...playerToSet,
							quote: {
								price: quoteData.price,
								previousPrice: quoteData.previousPrice ?? null,
								variationPercentage:
									quoteData.previousPrice != null && quoteData.previousPrice > 0
										? ((quoteData.price - quoteData.previousPrice) /
												quoteData.previousPrice) *
											100
										: null,
								effectiveDate: quoteData.effectiveDate,
							},
						};
					}
				} catch {
					// El error de falta de cotización se manejará por confirmationBlockReason
				}
			}

			setSelectedPlayer(playerToSet);
		},
		[form],
	);

	const selectedQuote: OfficialMarketQuote | null =
		selectedPlayer?.quote ?? null;

	const estimation: BuyEstimation | null = useMemo(
		() =>
			selectedQuote && form.validatedQuantity
				? calculateEstimation(form.validatedQuantity, selectedQuote.price)
				: null,
		[selectedQuote, form.validatedQuantity],
	);

	const isBusy = buyMutation.isPending || isSubmittingRef.current;

	const confirmationBlockReason = useMemo(() => {
		if (!authenticated) return "Iniciá sesión para comprar.";
		if (isSuperuser) return "Los administradores no pueden comprar tokens.";
		if (!selectedQuote) return "La cotización no está disponible.";
		if (!form.validatedQuantity)
			return "Ingresá una cantidad entera entre 1 y 100.";
		if (
			currentUser.data?.availableCreditsCentimes !== undefined &&
			estimation &&
			currentUser.data.availableCreditsCentimes < estimation.totalCentimes
		) {
			return "Créditos insuficientes para completar la compra.";
		}
		if (isBusy) return "La compra está en proceso.";
		if (isSuccess) return "La compra ya fue confirmada.";
		return null;
	}, [
		authenticated,
		isSuperuser,
		selectedQuote,
		form.validatedQuantity,
		currentUser.data?.availableCreditsCentimes,
		estimation,
		isBusy,
		isSuccess,
	]);

	const canConfirm = Boolean(
		authenticated &&
			!isSuperuser &&
			selectedQuote &&
			form.validatedQuantity &&
			!confirmationBlockReason &&
			!isBusy &&
			!isSuccess,
	);

	const handleConfirmBuy = useCallback(async () => {
		if (
			!authenticated ||
			isSuperuser ||
			!selectedPlayer ||
			!selectedQuote ||
			!form.validatedQuantity ||
			isBusy ||
			isSuccess
		) {
			return;
		}

		isSubmittingRef.current = true;
		setErrorMessage(null);

		try {
			const result = await buyMutation.mutateAsync({
				playerId: selectedPlayer.id,
				quantity: form.validatedQuantity,
			});

			await Promise.all([
				queryClient.invalidateQueries({
					queryKey: marketQueryKeys.officialAll(),
				}),
				queryClient.invalidateQueries({ queryKey: authQueryKeys.all }),
				queryClient.invalidateQueries({
					queryKey: portfolioQueryKeys.summary,
				}),
				queryClient.invalidateQueries({
					queryKey: portfolioQueryKeys.holdings("ALL", 0, 1).slice(0, 2),
				}),
				queryClient.invalidateQueries({
					queryKey: marketQueryKeys.trends(),
				}),
				queryClient.invalidateQueries({
					queryKey: marketQueryKeys.currentQuote(selectedPlayer.id),
				}),
				queryClient.invalidateQueries({
					queryKey: playersQueryKeys.quotes(selectedPlayer.id),
				}),
			]);

			notifySuccess(selectedPlayer.name, result);
			setPurchaseResult(result);
			setIsSuccess(true);
			autoCloseTimeoutRef.current = window.setTimeout(closeBuyModal, 2500);
		} catch (error: unknown) {
			const message = mapErrorCodeToMessage(error);
			setErrorMessage(message);
			notifyError(message);
		} finally {
			isSubmittingRef.current = false;
		}
	}, [
		authenticated,
		isSuperuser,
		selectedPlayer,
		selectedQuote,
		form.validatedQuantity,
		isBusy,
		isSuccess,
		buyMutation,
		queryClient,
		notifySuccess,
		notifyError,
		closeBuyModal,
	]);

	return {
		isModalOpen: Boolean(selectedPlayer?.quote),
		selectedPlayer,
		selectedQuote,
		estimation,
		formRegister: form.register,
		formError: form.formState.errors.quantity?.message,
		quantity: form.watch("quantity"),
		onQuantityChange: form.setQuantity,
		isBusy,
		canConfirm,
		isSuccess,
		purchaseResult,
		errorMessage,
		confirmationBlockReason,
		openBuyModal,
		closeBuyModal,
		handleConfirmBuy,
		triggerRef,
		initialFocusRef,
	};
}
