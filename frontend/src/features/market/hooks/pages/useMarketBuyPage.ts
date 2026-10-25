import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { getErrorCode } from "@/infrastructure/http";
import { AuthStatus, useAuthStore } from "@/infrastructure/storage";
import { marketQueryKeys } from "@/features/market/constants";
import { useBuyQuantityForm } from "@/features/market/form";
import { useBuyTokensMutation } from "@/features/market/hooks/mutations";
import { useMarketQuotesQuery } from "@/features/market/hooks/queries";
import { useMarketToast } from "@/features/market/hooks/useMarketToast";
import type {
	BuyEstimation,
	BuyResult,
	MarketPlayer,
} from "@/features/market/types";
import { calculateEstimation, getMarketQuoteMessage } from "@/features/market/utils";

interface UseMarketBuyPageParams {
	players: MarketPlayer[];
	onPurchaseSuccess?: (playerId: number) => void;
}

function mapErrorCodeToMessage(error: unknown): string {
	const code = getErrorCode(error);
	switch (code) {
		case "INVALID_REQUEST":
			return "La solicitud no es válida. Verificá los datos e intentá nuevamente.";
		case "INVALID_QUANTITY":
			return "Cantidad inválida.";
		case "INSUFFICIENT_CREDITS":
			return "Créditos insuficientes para completar la compra.";
		case "INSUFFICIENT_STOCK":
			return "No hay tokens disponibles para este jugador.";
		case "QUOTE_UNAVAILABLE":
		case "INVALID_QUOTE_PRICE":
			return "Cotización no disponible, intente más tarde.";
		case "SUPERUSER_CANNOT_BUY":
			return "Los administradores no pueden comprar tokens.";
		case "PLAYER_NOT_FOUND":
		case "PLAYER_NOT_VISIBLE":
			return "Jugador no disponible.";
		case "INTERNAL_ERROR":
		default:
			return "Error del servidor, intente más tarde.";
	}
}

/**
 * Hook de coordinación para MarketBuyPage (SPEC-019).
 * Coordina formulario, queries de cotizaciones reales, mutación de compra,
 * notificaciones, prevención de doble envío e invalidación de claves propias.
 */
export function useMarketBuyPage({
	players,
	onPurchaseSuccess,
}: UseMarketBuyPageParams) {
	const queryClient = useQueryClient();
	const { notifySuccess, notifyError } = useMarketToast();
	const authenticated = useAuthStore((state) =>
		state.status === AuthStatus.AUTHENTICATED && state.session !== null,
	);

	// IDs visibles de la página actual
	const playerIds = useMemo(() => players.map((p) => p.id), [players]);
	const quotesQuery = useMarketQuotesQuery(playerIds, authenticated);

	// Estado local de modal y selección
	const [selectedPlayer, setSelectedPlayer] = useState<MarketPlayer | null>(
		null,
	);
	const [isSuccess, setIsSuccess] = useState(false);
	const [purchaseResult, setPurchaseResult] = useState<BuyResult | null>(null);
	const [errorMessage, setErrorMessage] = useState<string | null>(null);

	// Referencias de foco
	const triggerRef = useRef<HTMLElement | null>(null);
	const initialFocusRef = useRef<HTMLInputElement | null>(null);

	// Prevención de doble envío síncrona
	const isSubmittingRef = useRef(false);
	const autoCloseTimeoutRef = useRef<number | null>(null);

	// Formulario de cantidad
	const form = useBuyQuantityForm();
	const { validatedQuantity, resetQuantity } = form;

	// Mutación de compra
	const buyMutation = useBuyTokensMutation();

	// Limpiar temporizador al desmontar
	useEffect(() => {
		return () => {
			if (autoCloseTimeoutRef.current !== null) {
				window.clearTimeout(autoCloseTimeoutRef.current);
			}
		};
	}, []);

	// Cotización del jugador seleccionado
	const selectedQuoteState = selectedPlayer
		? quotesQuery.getQuoteState(selectedPlayer.id)
		: null;
	const selectedQuote = selectedQuoteState?.quote ?? null;

	// Estimación calculada
	const estimation: BuyEstimation | null = useMemo(() => {
		if (!selectedQuote || !validatedQuantity || selectedQuote.price <= 0) {
			return null;
		}
		return calculateEstimation(validatedQuantity, selectedQuote.price);
	}, [selectedQuote, validatedQuantity]);

	// Abrir modal para un jugador
	const openBuyModal = useCallback(
		(player: MarketPlayer, triggerEl?: HTMLElement | null) => {
			if (autoCloseTimeoutRef.current !== null) {
				window.clearTimeout(autoCloseTimeoutRef.current);
				autoCloseTimeoutRef.current = null;
			}
			triggerRef.current = triggerEl ?? null;
			setSelectedPlayer(player);
			setIsSuccess(false);
			setPurchaseResult(null);
			setErrorMessage(null);
			isSubmittingRef.current = false;
			resetQuantity();
		},
		[resetQuantity],
	);

	// Cerrar modal
	const closeBuyModal = useCallback(() => {
		if (buyMutation.isPending || isSubmittingRef.current) {
			return; // Bloqueado durante submit
		}
		if (autoCloseTimeoutRef.current !== null) {
			window.clearTimeout(autoCloseTimeoutRef.current);
			autoCloseTimeoutRef.current = null;
		}
		setSelectedPlayer(null);
		setIsSuccess(false);
		setPurchaseResult(null);
		setErrorMessage(null);
		isSubmittingRef.current = false;
		resetQuantity();
	}, [buyMutation.isPending, resetQuantity]);

	// Ejecutar compra
	const handleConfirmBuy = useCallback(async () => {
		if (
			!authenticated ||
			selectedQuoteState?.status !== "available" ||
			isSubmittingRef.current ||
			buyMutation.isPending ||
			!selectedPlayer ||
			!validatedQuantity ||
			!selectedQuote ||
			selectedQuote.price <= 0
		) {
			return;
		}

		isSubmittingRef.current = true;
		setErrorMessage(null);

		try {
			const result = await buyMutation.mutateAsync({
				playerId: selectedPlayer.id,
				quantity: validatedQuantity,
			});

			// Éxito: invalidar cotización de market
			queryClient.invalidateQueries({
				queryKey: marketQueryKeys.currentQuote(selectedPlayer.id),
			});

			// Notificación toast compuesta con el nombre del jugador
			notifySuccess(selectedPlayer.name, result);

			// Callback hacia la app para invalidar Portfolio
			onPurchaseSuccess?.(selectedPlayer.id);

			setIsSuccess(true);
			setPurchaseResult(result);

			// Auto-cierre a los 2 segundos sin perder notificación
			autoCloseTimeoutRef.current = window.setTimeout(() => {
				closeBuyModal();
			}, 2000);
		} catch (error: unknown) {
			const msg = mapErrorCodeToMessage(error);
			setErrorMessage(msg);
			notifyError(msg);
		} finally {
			isSubmittingRef.current = false;
		}
	}, [
		authenticated,
		selectedQuoteState?.status,
		buyMutation,
		selectedPlayer,
		validatedQuantity,
		selectedQuote,
		queryClient,
		notifySuccess,
		onPurchaseSuccess,
		closeBuyModal,
		notifyError,
	]);

	const isBusy = buyMutation.isPending || isSubmittingRef.current;
	const canConfirm = Boolean(
		authenticated &&
			selectedQuoteState?.status === "available" &&
			validatedQuantity &&
			selectedQuote &&
			selectedQuote.price > 0 &&
			!isBusy &&
			!isSuccess,
	);
	const confirmationBlockReason = !authenticated
		? getMarketQuoteMessage("authentication-error")
		: isBusy ? "La compra está en proceso."
		: isSuccess ? "La compra ya fue confirmada."
		: selectedQuoteState?.status !== "available"
			? getMarketQuoteMessage(selectedQuoteState?.status ?? "loading", selectedQuoteState?.httpStatus)
		: !validatedQuantity ? "Ingresá una cantidad entera entre 1 y 100."
		: null;

	return {
		// Quotes query
		quotesQuery,
		// Modal state
		isModalOpen: Boolean(selectedPlayer),
		selectedPlayer,
		selectedQuote,
		selectedQuoteState,
		estimation,
		isSuccess,
		purchaseResult,
		errorMessage,
		isBusy,
		canConfirm,
		confirmationBlockReason,
		// Foco
		triggerRef,
		initialFocusRef,
		// Form
		form,
		// Actions
		openBuyModal,
		closeBuyModal,
		handleConfirmBuy,
	};
}
