import { useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { marketQueryKeys } from "@/features/market/constants";
import { useSellQuantityForm } from "@/features/market/form";
import { useSellTokensMutation } from "@/features/market/hooks/mutations";
import { useMarketQuotesQuery } from "@/features/market/hooks/queries";
import { useMarketToast } from "@/features/market/hooks/useMarketToast";
import { toMarketPlayer } from "@/features/market/market.mapper";
import type { PlayerHolding, SellResult } from "@/features/market/types";
import { useAllPlayersQuery } from "@/features/players";
import {
	portfolioQueryKeys,
	usePortfolioAllOpenHoldingsQuery,
} from "@/features/portfolio";
import {
	getApiError,
	getErrorCode,
	getErrorMessage,
} from "@/infrastructure/http";
import { AuthStatus, useAuthStore } from "@/infrastructure/storage";

interface Params {
	players?: PlayerHolding[];
	isLoadingPlayers?: boolean;
	isPlayersError?: boolean;
	errorMessage?: string | null;
	onRetryPlayers?: () => void;
	onSellSuccess?: (playerId: number) => void;
	onRefreshHoldings?: (playerId: number) => void;
}

function safeMessage(error: unknown): string {
	switch (getErrorCode(error)) {
		case "INVALID_REQUEST":
			return "La solicitud no es válida. Verificá los datos e intentá nuevamente.";
		case "INVALID_QUANTITY":
			return "Cantidad inválida.";
		case "INSUFFICIENT_STOCK":
			return "No hay tokens disponibles para este jugador.";
		case "QUOTE_UNAVAILABLE":
		case "INVALID_QUOTE_PRICE":
			return "Cotización no disponible, intente más tarde.";
		case "SUPERUSER_CANNOT_SELL":
			return "Los administradores no pueden publicar tokens.";
		case "PLAYER_NOT_FOUND":
		case "PLAYER_NOT_VISIBLE":
			return "Jugador no disponible.";
		default:
			return "Error del servidor, intente más tarde.";
	}
}

export function useMarketSellPage({
	players: externalPlayers,
	isLoadingPlayers: externalIsLoading,
	isPlayersError: externalIsError,
	errorMessage: externalErrorMessage,
	onRetryPlayers,
	onSellSuccess,
	onRefreshHoldings,
}: Params = {}) {
	const queryClient = useQueryClient();
	const { notifySellSuccess } = useMarketToast();
	const authenticated = useAuthStore(
		(state) =>
			state.status === AuthStatus.AUTHENTICATED && state.session !== null,
	);

	const playersQuery = useAllPlayersQuery();
	const holdingsQuery = usePortfolioAllOpenHoldingsQuery();

	const holdingsById = useMemo(
		() =>
			new Map(
				(holdingsQuery.data?.positions ?? []).map((position) => [
					position.playerId,
					position,
				]),
			),
		[holdingsQuery.data],
	);

	const internalHoldings: PlayerHolding[] = useMemo(() => {
		return (playersQuery.data?.players ?? []).flatMap((player) => {
			const holding = holdingsById.get(player.id);
			return holding
				? [
						{
							...toMarketPlayer(player),
							totalQuantity: holding.quantity,
							availableQuantity: holding.availableQuantity,
							reservedQuantity: holding.reservedQuantity,
						},
					]
				: [];
		});
	}, [playersQuery.data?.players, holdingsById]);

	const allHoldings = externalPlayers ?? internalHoldings;

	const refreshHoldings = useCallback(() => {
		const prefix = portfolioQueryKeys.holdings("OPEN", 0, 1).slice(0, 2);
		void queryClient.invalidateQueries({ queryKey: prefix });
	}, [queryClient]);

	const isLoadingPlayers =
		externalIsLoading ??
		(!externalPlayers &&
			(playersQuery.isPending ||
				playersQuery.isFetching ||
				holdingsQuery.isPending ||
				holdingsQuery.isFetching));

	const isPlayersError =
		externalIsError ??
		(!externalPlayers && (playersQuery.isError || holdingsQuery.isError));

	const playersErrorMessage =
		externalErrorMessage !== undefined
			? externalErrorMessage
			: isPlayersError
				? getErrorMessage(
						playersQuery.error ?? holdingsQuery.error,
						"No se pudo cargar el mercado y las tenencias.",
					)
				: null;

	const retryPlayers = useCallback(() => {
		if (onRetryPlayers) {
			onRetryPlayers();
		} else {
			void playersQuery.refetch();
			void holdingsQuery.refetch();
		}
	}, [onRetryPlayers, playersQuery, holdingsQuery]);

	const [selectedPlayer, setSelectedPlayer] = useState<PlayerHolding | null>(
		null,
	);
	useEffect(() => {
		if (!selectedPlayer) return;
		const refreshed = allHoldings.find(
			(player) => player.id === selectedPlayer.id,
		);
		if (
			refreshed &&
			(refreshed.totalQuantity !== selectedPlayer.totalQuantity ||
				refreshed.availableQuantity !== selectedPlayer.availableQuantity ||
				refreshed.reservedQuantity !== selectedPlayer.reservedQuantity)
		)
			setSelectedPlayer(refreshed);
	}, [allHoldings, selectedPlayer]);

	const [result, setResult] = useState<SellResult | null>(null);
	const [errorMessage, setErrorMessage] = useState<string | null>(null);
	const [searchInput, setSearchInput] = useState("");
	const [filters, setFilters] = useState({
		search: "",
		league: "all",
		position: "all",
		availability: "all",
		sort: "name,asc",
	});
	const [page, setPage] = useState(0);
	const [isRevalidating, setIsRevalidating] = useState(false);
	const submittingRef = useRef(false);
	const triggerRef = useRef<HTMLElement | null>(null);
	const initialFocusRef = useRef<HTMLInputElement | null>(null);
	const form = useSellQuantityForm(selectedPlayer?.availableQuantity ?? 0);
	const mutation = useSellTokensMutation();
	const quantity = form.validatedQuantity;

	const leagues = useMemo(
		() => [...new Set(allHoldings.map((player) => player.leagueName))].sort(),
		[allHoldings],
	);
	const positions = useMemo(
		() => [...new Set(allHoldings.map((player) => player.position))].sort(),
		[allHoldings],
	);
	const filteredPlayers = useMemo(() => {
		const filtered = allHoldings.filter((player) => {
			const search = filters.search.trim().toLocaleLowerCase("es-AR");
			return (
				(!search ||
					`${player.name} ${player.teamName}`
						.toLocaleLowerCase("es-AR")
						.includes(search)) &&
				(filters.league === "all" || player.leagueName === filters.league) &&
				(filters.position === "all" || player.position === filters.position) &&
				(filters.availability === "all" ||
					(filters.availability === "available"
						? player.availableQuantity > 0
						: player.reservedQuantity > 0))
			);
		});
		const [key, direction = "asc"] = filters.sort.split(",");
		filtered.sort((a, b) => {
			const av =
				key === "total"
					? a.totalQuantity
					: key === "available"
						? a.availableQuantity
						: key === "reserved"
							? a.reservedQuantity
							: a.name;
			const bv =
				key === "total"
					? b.totalQuantity
					: key === "available"
						? b.availableQuantity
						: key === "reserved"
							? b.reservedQuantity
							: b.name;
			const comparison =
				typeof av === "number" && typeof bv === "number"
					? av - bv
					: String(av).localeCompare(String(bv), "es");
			return direction === "desc" ? -comparison : comparison;
		});
		return filtered;
	}, [allHoldings, filters]);

	const pageSize = 12;
	const totalPages = Math.ceil(filteredPlayers.length / pageSize);
	const visiblePlayers = filteredPlayers.slice(
		page * pageSize,
		(page + 1) * pageSize,
	);
	const quoteQuery = useMarketQuotesQuery(
		visiblePlayers.map(({ id }) => id),
		authenticated,
	);
	const selectedQuoteState = selectedPlayer
		? quoteQuery.getQuoteState(selectedPlayer.id)
		: null;
	const selectedQuote = selectedQuoteState?.quote ?? null;
	const estimation = useMemo(
		() =>
			selectedQuote && quantity
				? {
						quantity,
						unitPriceCentimes: selectedQuote.price,
						totalCentimes: quantity * selectedQuote.price,
					}
				: null,
		[selectedQuote, quantity],
	);

	const open = useCallback(
		(player: PlayerHolding, trigger: HTMLElement | null) => {
			triggerRef.current = trigger;
			setSelectedPlayer(player);
			setResult(null);
			setErrorMessage(null);
			form.resetQuantity();
			refreshHoldings();
			onRefreshHoldings?.(player.id);
		},
		[form, onRefreshHoldings, refreshHoldings],
	);

	const close = useCallback(() => {
		if (mutation.isPending || submittingRef.current) return;
		setSelectedPlayer(null);
		setResult(null);
		setErrorMessage(null);
		form.resetQuantity();
	}, [form, mutation.isPending]);

	const confirm = useCallback(async () => {
		if (
			!authenticated ||
			!selectedPlayer ||
			!quantity ||
			!selectedQuote ||
			selectedQuoteState?.status !== "available" ||
			mutation.isPending ||
			submittingRef.current ||
			result
		)
			return;
		submittingRef.current = true;
		setIsRevalidating(true);
		setErrorMessage(null);
		let postStarted = false;
		try {
			const latestResult = await selectedQuoteState.refetch();
			const latest = latestResult.data;
			if (latestResult.isError || !latest || latest.price <= 0)
				throw new Error("quote-unavailable");
			if (latest.price !== selectedQuote.price) {
				setErrorMessage(
					"La cotización cambió. Revisá la nueva estimación y confirmá nuevamente.",
				);
				return;
			}
			postStarted = true;
			const sellResult = await mutation.mutateAsync({
				playerId: selectedPlayer.id,
				quantity,
			});
			setResult(sellResult);
			notifySellSuccess(selectedPlayer.name, sellResult);
			queryClient.invalidateQueries({
				queryKey: marketQueryKeys.currentQuote(selectedPlayer.id),
			});
			queryClient.invalidateQueries({
				queryKey: portfolioQueryKeys.summary,
			});
			refreshHoldings();
			onSellSuccess?.(selectedPlayer.id);
		} catch (error: unknown) {
			setErrorMessage(
				!postStarted &&
					error instanceof Error &&
					error.name === "MarketQuoteReadError"
					? "No se pudo revalidar la cotización. Revisá el estado e intentá nuevamente."
					: error instanceof Error && error.message === "quote-unavailable"
						? "Cotización no disponible, intente más tarde."
						: safeMessage(error),
			);
			if (
				postStarted &&
				!(axios.isAxiosError(error) && error.response?.status === 401) &&
				getApiError(error)?.status !== 401
			) {
				refreshHoldings();
				onRefreshHoldings?.(selectedPlayer.id);
			}
		} finally {
			submittingRef.current = false;
			setIsRevalidating(false);
		}
	}, [
		authenticated,
		selectedPlayer,
		quantity,
		selectedQuote,
		selectedQuoteState?.status,
		selectedQuoteState?.refetch,
		mutation,
		result,
		queryClient,
		refreshHoldings,
		onSellSuccess,
		onRefreshHoldings,
		notifySellSuccess,
	]);

	return {
		allHoldings,
		players: visiblePlayers,
		totalPlayers: filteredPlayers.length,
		page,
		pageSize,
		totalPages,
		setPage,
		leagues,
		positions,
		searchInput,
		setSearchInput,
		filters,
		applyFilters: () => {
			setFilters((current) => ({ ...current, search: searchInput }));
			setPage(0);
		},
		updateFilter: (
			key: "league" | "position" | "availability" | "sort",
			value: string,
		) => {
			setFilters((current) => ({ ...current, [key]: value }));
			setPage(0);
		},
		clearFilters: () => {
			setSearchInput("");
			setFilters({
				search: "",
				league: "all",
				position: "all",
				availability: "all",
				sort: "name,asc",
			});
			setPage(0);
		},
		quoteQuery,
		selectedPlayer,
		selectedQuote,
		selectedQuoteState,
		estimation,
		form,
		isOpen: Boolean(selectedPlayer),
		result,
		errorMessage,
		isBusy: mutation.isPending || isRevalidating,
		canConfirm: Boolean(
			authenticated &&
				quantity &&
				selectedQuoteState?.status === "available" &&
				selectedQuote &&
				!mutation.isPending &&
				!submittingRef.current &&
				!result,
		),
		confirmationBlockReason: !authenticated
			? "Iniciá sesión para publicar."
			: !quantity
				? "Cantidad entre 1 y 100"
				: selectedQuoteState?.status !== "available"
					? "La cotización no está disponible."
					: null,
		triggerRef,
		initialFocusRef,
		open,
		close,
		confirm,
		isLoadingPlayers,
		isPlayersError,
		playersErrorMessage,
		retryPlayers,
	};
}
