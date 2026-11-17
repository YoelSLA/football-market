import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { authQueryKeys, useCurrentUserQuery } from "@/features/auth";
import {
	COMMUNITY_MARKET_PAGE_SIZE,
	MARKET_SEARCH_DEBOUNCE_MS,
	marketQueryKeys,
} from "@/features/market/constants";
import { useBuyListingMutation } from "@/features/market/hooks/mutations";
import { useCommunityMarketQuery } from "@/features/market/hooks/queries";
import { useMarketToast } from "@/features/market/hooks/useMarketToast";
import { toMarketPlayer } from "@/features/market/market.mapper";
import type {
	BuyResult,
	CommunityMarketFilters,
	CommunityMarketPlayer,
	MarketListing,
} from "@/features/market/types";
import {
	buildCommunityPlayers,
	filterCommunityPlayers,
	parseCommunityMarketCriteria,
	serializeCommunityMarketCriteria,
} from "@/features/market/utils";
import { useAllPlayersQuery } from "@/features/players";
import { portfolioQueryKeys } from "@/features/portfolio";
import { getErrorCode, getErrorMessage } from "@/infrastructure/http";
import { useAuthStore } from "@/infrastructure/storage";

function p2pErrorMessage(error: unknown) {
	switch (getErrorCode(error)) {
		case "LISTING_NOT_AVAILABLE":
			return "La oferta dejó de estar disponible. Actualizamos las publicaciones.";
		case "LISTING_NOT_FOUND":
			return "La publicación ya no está disponible.";
		case "CANNOT_BUY_OWN_LISTING":
			return "No podés comprar una publicación propia.";
		case "INSUFFICIENT_CREDITS":
			return "Créditos insuficientes para completar la compra.";
		case "INSUFFICIENT_STOCK":
			return "El lote ya no puede completarse.";
		case "SUPERUSER_CANNOT_BUY":
			return "Los administradores no pueden comprar tokens.";
		case "INVALID_REQUEST":
			return "La solicitud de compra no es válida.";
		default:
			return "No pudimos confirmar el resultado. Revisá tus operaciones antes de reintentar.";
	}
}

export function useCommunityMarketPage(
	enabled = true,
	onPurchaseSuccess?: (playerId: number) => void,
) {
	const queryClient = useQueryClient();
	const { notifySuccess, notifyError } = useMarketToast();
	const revision = useAuthStore((state) => state.revision);
	const currentUser = useCurrentUserQuery(true, revision);
	const [searchParams, setSearchParams] = useSearchParams();
	const urlState = useMemo(
		() => parseCommunityMarketCriteria(searchParams),
		[searchParams],
	);
	const [searchDraft, setSearchDraft] = useState(urlState.search);
	const listingsQuery = useCommunityMarketQuery(enabled);
	const playersQuery = useAllPlayersQuery();
	const buyMutation = useBuyListingMutation();
	const [selectedListing, setSelectedListing] = useState<MarketListing | null>(
		null,
	);
	const [purchasePlayer, setPurchasePlayer] =
		useState<CommunityMarketPlayer | null>(null);
	const [purchaseResult, setPurchaseResult] = useState<BuyResult | null>(null);
	const [purchaseError, setPurchaseError] = useState<string | null>(null);
	const submittingRef = useRef(false);
	const triggerRef = useRef<HTMLElement | null>(null);

	const replaceUrlState = useCallback(
		(next: typeof urlState) =>
			setSearchParams(serializeCommunityMarketCriteria(next), {
				replace: true,
			}),
		[setSearchParams],
	);

	useEffect(() => setSearchDraft(urlState.search), [urlState.search]);
	useEffect(() => {
		if (searchDraft.trim() === urlState.search) return;
		const timeout = window.setTimeout(() => {
			replaceUrlState({
				...urlState,
				search: searchDraft.trim(),
				page: 0,
			});
		}, MARKET_SEARCH_DEBOUNCE_MS);
		return () => window.clearTimeout(timeout);
	}, [replaceUrlState, searchDraft, urlState]);

	const communityPlayers = useMemo(() => {
		if (!listingsQuery.data || !playersQuery.data) return [];
		return buildCommunityPlayers(
			listingsQuery.data,
			playersQuery.data.players.map(toMarketPlayer),
		);
	}, [listingsQuery.data, playersQuery.data]);
	const filters: CommunityMarketFilters = useMemo(
		() => ({
			search: urlState.search,
			league: urlState.league,
			position: urlState.position,
			sort: urlState.sort,
		}),
		[urlState],
	);
	const filteredPlayers = useMemo(
		() => filterCommunityPlayers(communityPlayers, filters),
		[communityPlayers, filters],
	);
	const totalPages = Math.ceil(
		filteredPlayers.length / COMMUNITY_MARKET_PAGE_SIZE,
	);
	const visiblePlayers = filteredPlayers.slice(
		urlState.page * COMMUNITY_MARKET_PAGE_SIZE,
		(urlState.page + 1) * COMMUNITY_MARKET_PAGE_SIZE,
	);
	const selectedPlayer =
		urlState.player === null
			? null
			: (communityPlayers.find((player) => player.id === urlState.player) ??
				null);
	const availableLeagues = useMemo(
		() =>
			[...new Set(communityPlayers.map((player) => player.leagueName))].sort(),
		[communityPlayers],
	);
	const availablePositions = useMemo(
		() =>
			[...new Set(communityPlayers.map((player) => player.position))].sort(),
		[communityPlayers],
	);
	const isLoading = listingsQuery.isPending || playersQuery.isPending;
	const isUpdating =
		!isLoading && (listingsQuery.isFetching || playersQuery.isFetching);
	const isError = listingsQuery.isError || playersQuery.isError;

	useEffect(() => {
		if (isLoading || listingsQuery.isError || playersQuery.isError) return;
		const leagueValid =
			urlState.league === "all" || availableLeagues.includes(urlState.league);
		const positionValid =
			urlState.position === "all" ||
			availablePositions.includes(urlState.position);
		if (!leagueValid || !positionValid)
			replaceUrlState({
				...urlState,
				league: leagueValid ? urlState.league : "all",
				position: positionValid ? urlState.position : "all",
				page: 0,
			});
	}, [
		availableLeagues,
		availablePositions,
		isLoading,
		listingsQuery.isError,
		playersQuery.isError,
		replaceUrlState,
		urlState,
	]);

	useEffect(() => {
		if (listingsQuery.isFetching || playersQuery.isFetching) return;
		const lastPage = Math.max(0, totalPages - 1);
		if (urlState.page > lastPage)
			replaceUrlState({ ...urlState, page: lastPage });
	}, [
		listingsQuery.isFetching,
		playersQuery.isFetching,
		replaceUrlState,
		totalPages,
		urlState,
	]);

	const updateFilters = useCallback(
		(next: Partial<CommunityMarketFilters>) => {
			if (next.search !== undefined) {
				setSearchDraft(next.search);
				return;
			}
			replaceUrlState({ ...urlState, ...next, page: 0 });
		},
		[replaceUrlState, urlState],
	);
	const clearFilters = useCallback(() => {
		setSearchDraft("");
		replaceUrlState({
			...urlState,
			search: "",
			league: "all",
			position: "all",
			sort: "price",
			page: 0,
		});
	}, [replaceUrlState, urlState]);
	const openPlayer = useCallback(
		(player: CommunityMarketPlayer) =>
			replaceUrlState({ ...urlState, player: player.id }),
		[replaceUrlState, urlState],
	);
	const closePlayer = useCallback(
		() => replaceUrlState({ ...urlState, player: null }),
		[replaceUrlState, urlState],
	);
	const openListing = useCallback(
		(listing: MarketListing, trigger: HTMLElement | null) => {
			if (!selectedPlayer || Date.parse(listing.expiresAt) <= Date.now())
				return;
			triggerRef.current = trigger;
			setSelectedListing(listing);
			setPurchasePlayer(selectedPlayer);
			setPurchaseResult(null);
			setPurchaseError(null);
		},
		[selectedPlayer],
	);
	const closeListing = useCallback(() => {
		if (buyMutation.isPending || submittingRef.current) return;
		setSelectedListing(null);
		setPurchasePlayer(null);
		setPurchaseResult(null);
		setPurchaseError(null);
	}, [buyMutation.isPending]);
	const retry = useCallback(async () => {
		await Promise.all([listingsQuery.refetch(), playersQuery.refetch()]);
	}, [listingsQuery, playersQuery]);
	const confirmListing = useCallback(async () => {
		if (
			!selectedListing ||
			buyMutation.isPending ||
			submittingRef.current ||
			purchaseResult ||
			Date.parse(selectedListing.expiresAt) <= Date.now() ||
			(currentUser.data?.availableCredits !== undefined &&
				currentUser.data.availableCredits < selectedListing.totalAmount)
		)
			return;
		submittingRef.current = true;
		setPurchaseError(null);
		try {
			const result = await buyMutation.mutateAsync(selectedListing.listingId);
			setPurchaseResult(result);
			notifySuccess(purchasePlayer?.name ?? "Jugador", result);
			await Promise.all([
				queryClient.invalidateQueries({ queryKey: marketQueryKeys.listings() }),
				queryClient.invalidateQueries({ queryKey: authQueryKeys.all }),
				queryClient.invalidateQueries({
					queryKey: portfolioQueryKeys.summary,
				}),
				queryClient.invalidateQueries({
					queryKey: portfolioQueryKeys.holdings("ALL", 0, 1).slice(0, 2),
				}),
			]);
			onPurchaseSuccess?.(result.playerId);
		} catch (error: unknown) {
			const message = p2pErrorMessage(error);
			setPurchaseError(message);
			notifyError(message);
			if (
				[
					"LISTING_NOT_AVAILABLE",
					"LISTING_NOT_FOUND",
					"CANNOT_BUY_OWN_LISTING",
					"INSUFFICIENT_STOCK",
				].includes(getErrorCode(error) ?? "")
			)
				await queryClient.invalidateQueries({
					queryKey: marketQueryKeys.listings(),
				});
		} finally {
			submittingRef.current = false;
		}
	}, [
		buyMutation,
		currentUser.data?.availableCredits,
		notifyError,
		notifySuccess,
		onPurchaseSuccess,
		purchasePlayer?.name,
		purchaseResult,
		queryClient,
		selectedListing,
	]);

	return {
		filters: { ...filters, search: searchDraft },
		updateFilters,
		clearFilters,
		leagues: availableLeagues,
		positions: availablePositions,
		players: visiblePlayers,
		totalPlayers: filteredPlayers.length,
		availableCredits: currentUser.data?.availableCredits,
		page: urlState.page,
		totalPages,
		setPage: (page: number) => replaceUrlState({ ...urlState, page }),
		selectedPlayerId: urlState.player,
		selectedPlayer,
		openPlayer,
		closePlayer,
		selectedListing,
		purchasePlayer,
		openListing,
		closeListing,
		confirmListing,
		purchaseResult,
		purchaseError,
		canConfirmListing: Boolean(
			selectedListing &&
				!purchaseResult &&
				Date.parse(selectedListing.expiresAt) > Date.now() &&
				(currentUser.data?.availableCredits === undefined ||
					currentUser.data.availableCredits >= selectedListing.totalAmount),
		),
		isBuying: buyMutation.isPending || submittingRef.current,
		isLoading,
		isUpdating,
		isError,
		errorMessage: isError
			? getErrorMessage(
					listingsQuery.error ?? playersQuery.error,
					"No pudimos cargar las ofertas de usuarios.",
				)
			: null,
		retry,
		triggerRef,
	};
}

export type CommunityMarketState = ReturnType<typeof useCommunityMarketPage>;
