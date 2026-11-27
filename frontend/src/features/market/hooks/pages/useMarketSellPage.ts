import { useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { authQueryKeys } from "@/features/auth";
import { marketQueryKeys } from "@/features/market/constants";
import { useSellQuantityForm } from "@/features/market/form";
import {
	useSellOfficialTokensMutation,
	useSellTokensMutation,
} from "@/features/market/hooks/mutations";
import { useMarketQuotesQuery } from "@/features/market/hooks/queries";
import { useMarketToast } from "@/features/market/hooks/useMarketToast";
import { toMarketPlayer } from "@/features/market/market.mapper";
import type {
	PlayerHolding,
	SellEstimation,
	SellMode,
	SellOfficialEstimation,
	SellOfficialResult,
	SellResult,
} from "@/features/market/types";
import {
	sanitizePositions,
	useAllPlayersQuery,
	usePlayerCatalogFiltersQuery,
} from "@/features/players";
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
		case "INSUFFICIENT_SUPERUSER_FUNDS":
		case "INSUFFICIENT_LIQUIDITY":
			return "La casa oficial no dispone de fondos suficientes en este momento.";
		case "PRICE_OUT_OF_BOUNDS":
			return "El precio acordado se encuentra fuera del margen permitido (-20% a +20%).";
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
	const { notifySellSuccess, notifySellOfficialSuccess } = useMarketToast();
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

	const [sellMode, setSellMode] = useState<SellMode>("OFFICIAL");
	const [p2pUnitPriceCentimes, setP2PUnitPriceCentimes] = useState<number>(0);
	const [result, setResult] = useState<SellResult | SellOfficialResult | null>(
		null,
	);
	const [errorMessage, setErrorMessage] = useState<string | null>(null);
	const [searchParams] = useSearchParams();
	const initialSearch = searchParams.get("search") ?? "";
	const [searchInput, setSearchInput] = useState(initialSearch);
	const filterOptionsQuery = usePlayerCatalogFiltersQuery();
	const filterOptions = filterOptionsQuery.data;
	const [filters, setFilters] = useState({
		search: initialSearch,
		league: "all",
		team: "all",
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
	const officialMutation = useSellOfficialTokensMutation();
	const quantity = form.validatedQuantity;

	const leagues = useMemo(
		() =>
			[
				...new Set(
					allHoldings
						.map((player) => player.leagueName)
						.filter(
							(name): name is string => name !== null && name !== undefined,
						),
				),
			].sort(),
		[allHoldings],
	);
	const positions = useMemo(
		() => sanitizePositions(allHoldings.map((player) => player.position)),
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
				(filters.league === "all" ||
					String(player.leagueId) === filters.league ||
					player.leagueName === filters.league) &&
				(filters.team === "all" ||
					String(player.teamId) === filters.team ||
					player.teamName === filters.team) &&
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

	useEffect(() => {
		if (!selectedPlayer) {
			setSellMode("OFFICIAL");
			setP2PUnitPriceCentimes(0);
			return;
		}
		if (
			selectedQuote &&
			selectedQuote.price > 0 &&
			p2pUnitPriceCentimes === 0
		) {
			setP2PUnitPriceCentimes(Math.round(selectedQuote.price * 0.9));
		}
	}, [selectedPlayer, selectedQuote, p2pUnitPriceCentimes]);

	const officialEstimation = useMemo((): SellOfficialEstimation | null => {
		if (!selectedQuote || !quantity || selectedQuote.price <= 0) return null;
		const discountPercentage = 10;
		const spreadCentimes = Math.round(selectedQuote.price * 0.1);
		const unitPriceNetCentimes = selectedQuote.price - spreadCentimes;
		return {
			quantity,
			officialPriceCentimes: selectedQuote.price,
			discountPercentage,
			spreadCentimes,
			unitPriceNetCentimes,
			totalNetCentimes: quantity * unitPriceNetCentimes,
		};
	}, [selectedQuote, quantity]);

	const minP2PPrice = selectedQuote ? Math.round(selectedQuote.price * 0.8) : 0;
	const maxP2PPrice = selectedQuote ? Math.round(selectedQuote.price * 1.2) : 0;
	const isP2PPriceValid =
		Boolean(selectedQuote) &&
		p2pUnitPriceCentimes >= minP2PPrice &&
		p2pUnitPriceCentimes <= maxP2PPrice &&
		p2pUnitPriceCentimes > 0;

	const estimation = useMemo((): SellEstimation | null => {
		if (!selectedQuote || !quantity || selectedQuote.price <= 0) return null;
		const unitPrice =
			sellMode === "OFFICIAL"
				? selectedQuote.price - Math.round(selectedQuote.price * 0.1)
				: p2pUnitPriceCentimes;
		return {
			quantity,
			unitPriceCentimes: unitPrice,
			totalCentimes: quantity * unitPrice,
		};
	}, [selectedQuote, quantity, sellMode, p2pUnitPriceCentimes]);

	const open = useCallback(
		(player: PlayerHolding, trigger: HTMLElement | null) => {
			triggerRef.current = trigger;
			setSelectedPlayer(player);
			setResult(null);
			setErrorMessage(null);
			setSellMode("OFFICIAL");
			setP2PUnitPriceCentimes(0);
			form.resetQuantity();
			refreshHoldings();
			onRefreshHoldings?.(player.id);
		},
		[form, onRefreshHoldings, refreshHoldings],
	);

	const close = useCallback(() => {
		if (
			mutation.isPending ||
			officialMutation.isPending ||
			submittingRef.current
		)
			return;
		setSelectedPlayer(null);
		setResult(null);
		setErrorMessage(null);
		setSellMode("OFFICIAL");
		setP2PUnitPriceCentimes(0);
		form.resetQuantity();
	}, [form, mutation.isPending, officialMutation.isPending]);

	const isBusy =
		mutation.isPending || officialMutation.isPending || isRevalidating;

	const canConfirm = Boolean(
		authenticated &&
			quantity &&
			quantity <= (selectedPlayer?.availableQuantity ?? 0) &&
			selectedQuoteState?.status === "available" &&
			selectedQuote &&
			!isBusy &&
			!submittingRef.current &&
			!result &&
			(sellMode === "OFFICIAL" ? true : isP2PPriceValid),
	);

	const confirmationBlockReason = !authenticated
		? "Iniciá sesión para publicar."
		: !quantity
			? "Cantidad entre 1 y 100"
			: quantity > (selectedPlayer?.availableQuantity ?? 0)
				? "Cantidad superior a los tokens disponibles."
				: selectedQuoteState?.status !== "available"
					? "La cotización no está disponible."
					: sellMode === "P2P" && !isP2PPriceValid
						? "El precio debe encontrarse entre -20% y +20% de la cotización oficial."
						: null;

	const confirm = useCallback(async () => {
		if (
			!authenticated ||
			!selectedPlayer ||
			!quantity ||
			!selectedQuote ||
			selectedQuoteState?.status !== "available" ||
			mutation.isPending ||
			officialMutation.isPending ||
			submittingRef.current ||
			result
		)
			return;

		if (sellMode === "P2P" && !isP2PPriceValid) return;

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

			if (sellMode === "OFFICIAL") {
				const officialResult = await officialMutation.mutateAsync({
					playerId: selectedPlayer.id,
					quantity,
				});
				setResult(officialResult);
				notifySellOfficialSuccess(selectedPlayer.name, officialResult);
				void queryClient.invalidateQueries({
					queryKey: authQueryKeys.all,
				});
				void queryClient.invalidateQueries({
					queryKey: marketQueryKeys.currentQuote(selectedPlayer.id),
				});
				void queryClient.invalidateQueries({
					queryKey: marketQueryKeys.all,
				});
				void queryClient.invalidateQueries({
					queryKey: portfolioQueryKeys.summary,
				});
				refreshHoldings();
				onSellSuccess?.(selectedPlayer.id);
			} else {
				const sellResult = await mutation.mutateAsync({
					playerId: selectedPlayer.id,
					quantity,
					unitPrice: p2pUnitPriceCentimes,
				});
				setResult(sellResult);
				notifySellSuccess(selectedPlayer.name, sellResult);
				void queryClient.invalidateQueries({
					queryKey: marketQueryKeys.currentQuote(selectedPlayer.id),
				});
				void queryClient.invalidateQueries({
					queryKey: marketQueryKeys.p2pAll(),
				});
				void queryClient.invalidateQueries({
					queryKey: portfolioQueryKeys.summary,
				});
				refreshHoldings();
				onSellSuccess?.(selectedPlayer.id);
			}
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
		officialMutation,
		result,
		sellMode,
		isP2PPriceValid,
		p2pUnitPriceCentimes,
		queryClient,
		refreshHoldings,
		onSellSuccess,
		onRefreshHoldings,
		notifySellSuccess,
		notifySellOfficialSuccess,
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
		setSearchInput: (value: string) => {
			setSearchInput(value);
			setFilters((current) => ({ ...current, search: value }));
			setPage(0);
		},
		filters,
		applyFilters: () => {
			setFilters((current) => ({ ...current, search: searchInput }));
			setPage(0);
		},
		updateFilter: (
			key: "league" | "team" | "position" | "availability" | "sort",
			value: string,
		) => {
			setFilters((current) => {
				const next = { ...current, [key]: value };
				if (key === "league") {
					next.team = "all";
				}
				return next;
			});
			setPage(0);
		},
		clearFilters: () => {
			setSearchInput("");
			setFilters({
				search: "",
				league: "all",
				team: "all",
				position: "all",
				availability: "all",
				sort: "name,asc",
			});
			setPage(0);
		},
		filterOptions,
		quoteQuery,
		selectedPlayer,
		selectedQuote,
		selectedQuoteState,
		estimation,
		officialEstimation,
		sellMode,
		setSellMode,
		p2pUnitPriceCentimes,
		setP2PUnitPriceCentimes,
		form,
		isOpen: Boolean(selectedPlayer),
		result,
		errorMessage,
		isBusy,
		canConfirm,
		confirmationBlockReason,
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
