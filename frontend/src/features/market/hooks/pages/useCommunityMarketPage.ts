import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { authQueryKeys, useCurrentUserQuery } from "@/features/auth";
import { marketQueryKeys } from "@/features/market/constants";
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
} from "@/features/market/utils";
import { useAllPlayersQuery } from "@/features/players";
import { portfolioQueryKeys } from "@/features/portfolio";
import { getErrorCode, getErrorMessage } from "@/infrastructure/http";
import { useAuthStore } from "@/infrastructure/storage";

const PAGE_SIZE = 12;
const INITIAL_FILTERS: CommunityMarketFilters = {
	search: "",
	league: "all",
	position: "all",
	sort: "price",
};

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
	const availableCredits = currentUser.data?.availableCredits;
	const [searchParams, setSearchParams] = useSearchParams();
	const listingsQuery = useCommunityMarketQuery(enabled);
	const playersQuery = useAllPlayersQuery();
	const buyMutation = useBuyListingMutation();
	const [filters, setFilters] = useState(INITIAL_FILTERS);
	const [page, setPage] = useState(0);
	const [selectedListing, setSelectedListing] = useState<MarketListing | null>(
		null,
	);
	const [purchaseResult, setPurchaseResult] = useState<BuyResult | null>(null);
	const [purchaseError, setPurchaseError] = useState<string | null>(null);
	const submittingRef = useRef(false);
	const triggerRef = useRef<HTMLElement | null>(null);

	const communityPlayers = useMemo(() => {
		if (!listingsQuery.data || !playersQuery.data) return [];
		return buildCommunityPlayers(
			listingsQuery.data,
			playersQuery.data.players.map(toMarketPlayer),
		);
	}, [listingsQuery.data, playersQuery.data]);
	const filteredPlayers = useMemo(
		() => filterCommunityPlayers(communityPlayers, filters),
		[communityPlayers, filters],
	);
	const totalPages = Math.ceil(filteredPlayers.length / PAGE_SIZE);
	const visiblePlayers = filteredPlayers.slice(
		page * PAGE_SIZE,
		(page + 1) * PAGE_SIZE,
	);
	const playerId = Number(searchParams.get("player"));
	const selectedPlayer = Number.isSafeInteger(playerId)
		? (communityPlayers.find((player) => player.id === playerId) ?? null)
		: null;
	const isLoading =
		listingsQuery.isPending ||
		playersQuery.isPending ||
		playersQuery.isFetching;
	const isError = listingsQuery.isError || playersQuery.isError;
	const errorMessage = isError
		? getErrorMessage(
				listingsQuery.error ?? playersQuery.error,
				"No pudimos cargar las ofertas de usuarios.",
			)
		: null;

	const updateFilters = useCallback((next: Partial<CommunityMarketFilters>) => {
		setFilters((current) => ({ ...current, ...next }));
		setPage(0);
	}, []);
	const openPlayer = useCallback(
		(player: CommunityMarketPlayer) => {
			const next = new URLSearchParams(searchParams);
			next.set("player", String(player.id));
			setSearchParams(next);
		},
		[searchParams, setSearchParams],
	);
	const closePlayer = useCallback(() => {
		const next = new URLSearchParams(searchParams);
		next.delete("player");
		setSearchParams(next);
	}, [searchParams, setSearchParams]);
	const openListing = useCallback(
		(listing: MarketListing, trigger: HTMLElement | null) => {
			triggerRef.current = trigger;
			setSelectedListing(listing);
			setPurchaseResult(null);
			setPurchaseError(null);
		},
		[],
	);
	const closeListing = useCallback(() => {
		if (buyMutation.isPending || submittingRef.current) return;
		setSelectedListing(null);
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
			purchaseResult
		)
			return;
		submittingRef.current = true;
		setPurchaseError(null);
		try {
			const result = await buyMutation.mutateAsync(selectedListing.listingId);
			setPurchaseResult(result);
			notifySuccess(selectedPlayer?.name ?? "Jugador", result);
			await Promise.all([
				queryClient.invalidateQueries({ queryKey: marketQueryKeys.listings() }),
				queryClient.invalidateQueries({ queryKey: authQueryKeys.all }),
				queryClient.invalidateQueries({ queryKey: portfolioQueryKeys.summary }),
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
			) {
				await queryClient.invalidateQueries({
					queryKey: marketQueryKeys.listings(),
				});
			}
		} finally {
			submittingRef.current = false;
		}
	}, [
		buyMutation,
		notifyError,
		notifySuccess,
		onPurchaseSuccess,
		purchaseResult,
		queryClient,
		selectedListing,
		selectedPlayer?.name,
	]);

	return {
		filters,
		updateFilters,
		clearFilters: () => {
			setFilters(INITIAL_FILTERS);
			setPage(0);
		},
		leagues: [
			...new Set(communityPlayers.map((player) => player.leagueName)),
		].sort(),
		positions: [
			...new Set(communityPlayers.map((player) => player.position)),
		].sort(),
		players: visiblePlayers,
		totalPlayers: filteredPlayers.length,
		availableCredits,
		page,
		totalPages,
		setPage,
		selectedPlayer,
		openPlayer,
		closePlayer,
		selectedListing,
		openListing,
		closeListing,
		confirmListing,
		purchaseResult,
		purchaseError,
		isBuying: buyMutation.isPending || submittingRef.current,
		isLoading,
		isError,
		errorMessage,
		retry,
		triggerRef,
	};
}

export type CommunityMarketState = ReturnType<typeof useCommunityMarketPage>;
