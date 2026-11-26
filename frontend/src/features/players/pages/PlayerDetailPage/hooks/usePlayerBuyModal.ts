import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useMemo, useRef, useState } from "react";
import { authQueryKeys, useCurrentUserQuery } from "@/features/auth";
import { marketQueryKeys } from "@/features/market/constants";
import { useBuyQuantityForm } from "@/features/market/form";
import { useBuyTokensMutation } from "@/features/market/hooks/mutations";
import { useMarketToast } from "@/features/market/hooks/useMarketToast";
import type {
	BuyEstimation,
	BuyResult,
	OfficialMarketPlayer,
	OfficialMarketQuote,
} from "@/features/market/types";
import { calculateEstimation } from "@/features/market/utils";
import { playersQueryKeys } from "@/features/players/constants";
import type { PlayerDetail, PlayerQuote } from "@/features/players/types";
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
 * Hook que gestiona el diálogo modal y la mutación de compra de tokens directamente
 * desde la página de detalle del jugador.
 */
export function usePlayerBuyModal(
	player: PlayerDetail | undefined,
	quote: PlayerQuote | null | undefined,
) {
	const queryClient = useQueryClient();
	const { notifySuccess, notifyError } = useMarketToast();
	const [isOpen, setIsOpen] = useState(false);
	const [purchaseResult, setPurchaseResult] = useState<BuyResult | null>(null);
	const [errorMessage, setErrorMessage] = useState<string | null>(null);
	const [isSuccess, setIsSuccess] = useState(false);
	const isSubmittingRef = useRef(false);
	const triggerRef = useRef<HTMLElement | null>(null);
	const initialFocusRef = useRef<HTMLInputElement | null>(null);

	const revision = useAuthStore((state) => state.revision);
	const authenticated = useAuthStore(
		(state) => state.status === AuthStatus.AUTHENTICATED,
	);
	const currentUser = useCurrentUserQuery(authenticated, revision);
	const isSuperuser = currentUser.data?.role === "SUPERUSER";

	const form = useBuyQuantityForm();
	const buyMutation = useBuyTokensMutation();

	const officialPlayer: OfficialMarketPlayer | null = useMemo(() => {
		if (!player) return null;
		return {
			id: player.id,
			name: player.name,
			teamId: player.teamId,
			teamName: player.teamName,
			leagueId: player.leagueId,
			leagueName: player.leagueName,
			position: player.position,
			nationality: player.nationality,
			dateOfBirth: player.dateOfBirth,
			imageUrl: player.imageUrl,
			fallbackImageUrl: player.fallbackImageUrl,
			teamBadgeUrl: null,
			quote: quote
				? {
						price: quote.price,
						previousPrice: quote.previousPrice,
						variationPercentage: quote.percentageChange,
						effectiveDate: quote.effectiveDate,
					}
				: null,
		};
	}, [player, quote]);

	const officialQuote: OfficialMarketQuote | null =
		officialPlayer?.quote ?? null;

	const estimation: BuyEstimation | null = useMemo(() => {
		return officialQuote && form.validatedQuantity
			? calculateEstimation(form.validatedQuantity, officialQuote.price)
			: null;
	}, [officialQuote, form.validatedQuantity]);

	const openBuyModal = useCallback(
		(triggerEl?: HTMLElement | null) => {
			if (triggerEl) triggerRef.current = triggerEl;
			setPurchaseResult(null);
			setErrorMessage(null);
			setIsSuccess(false);
			isSubmittingRef.current = false;
			form.resetQuantity();
			setIsOpen(true);
		},
		[form],
	);

	const closeBuyModal = useCallback(() => {
		if (buyMutation.isPending || isSubmittingRef.current) return;
		setIsOpen(false);
		setPurchaseResult(null);
		setErrorMessage(null);
		setIsSuccess(false);
		form.resetQuantity();
	}, [buyMutation.isPending, form]);

	const confirmationBlockReason = useMemo(() => {
		if (!authenticated) return "Debes iniciar sesión para operar.";
		if (isSuperuser) return "Los administradores no pueden comprar tokens.";
		if (!officialQuote) return "No hay cotización oficial disponible.";
		if (
			currentUser.data?.availableCreditsCentimes !== undefined &&
			estimation &&
			currentUser.data.availableCreditsCentimes < estimation.totalCentimes
		) {
			return "Créditos insuficientes para completar la compra.";
		}
		return null;
	}, [
		authenticated,
		isSuperuser,
		officialQuote,
		currentUser.data?.availableCreditsCentimes,
		estimation,
	]);

	const canConfirm = Boolean(
		authenticated &&
			!isSuperuser &&
			form.validatedQuantity &&
			officialQuote &&
			!confirmationBlockReason &&
			!buyMutation.isPending &&
			!isSubmittingRef.current &&
			!isSuccess,
	);

	const handleConfirmBuy = useCallback(async () => {
		if (!player || !canConfirm || !form.validatedQuantity) return;

		isSubmittingRef.current = true;
		setErrorMessage(null);

		try {
			const result = await buyMutation.mutateAsync({
				playerId: player.id,
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
					queryKey: playersQueryKeys.quotes(player.id),
				}),
			]);

			setPurchaseResult(result);
			setIsSuccess(true);
			notifySuccess(player.name, result);
		} catch (error) {
			const message = mapErrorCodeToMessage(error);
			setErrorMessage(message);
			notifyError(message);
		} finally {
			isSubmittingRef.current = false;
		}
	}, [
		player,
		canConfirm,
		form.validatedQuantity,
		buyMutation,
		queryClient,
		notifySuccess,
		notifyError,
	]);

	return {
		isOpen,
		openBuyModal,
		closeBuyModal,
		officialPlayer,
		officialQuote,
		estimation,
		formRegister: form.register,
		formError: form.formState.errors.quantity?.message,
		quantity: form.watch("quantity"),
		onQuantityChange: form.setQuantity,
		isBusy: buyMutation.isPending,
		canConfirm,
		isSuccess,
		purchaseResult,
		errorMessage,
		confirmationBlockReason,
		handleConfirmBuy,
		triggerRef,
		initialFocusRef,
	};
}
