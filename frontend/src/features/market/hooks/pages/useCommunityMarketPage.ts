import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { authQueryKeys, useCurrentUserQuery } from "@/features/auth";
import {
	MARKET_SEARCH_DEBOUNCE_MS,
	marketQueryKeys,
} from "@/features/market/constants";
import { useBuyListingMutation } from "@/features/market/hooks/mutations";
import {
	useMarketQuotesQuery,
	useP2PMarketQuery,
	usePlayerListingsQuery,
} from "@/features/market/hooks/queries";
import { useMarketToast } from "@/features/market/hooks/useMarketToast";
import { toMarketPlayer } from "@/features/market/market.mapper";
import type {
	BuyResult,
	CommunityMarketCriteria,
	CommunityMarketFilters,
	CommunityMarketPlayer,
	MarketListing,
	MarketQuote,
} from "@/features/market/types";
import {
	parseCommunityMarketCriteria,
	serializeCommunityMarketCriteria,
} from "@/features/market/utils";
import { sanitizePositions, useAllPlayersQuery } from "@/features/players";
import { portfolioQueryKeys } from "@/features/portfolio";
import { getErrorCode, getErrorMessage } from "@/infrastructure/http";
import { AuthStatus, useAuthStore } from "@/infrastructure/storage";

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
	const authenticated = useAuthStore(
		(state) =>
			state.status === AuthStatus.AUTHENTICATED && state.session !== null,
	);
	const currentUser = useCurrentUserQuery(true, revision);
	const [searchParams, setSearchParams] = useSearchParams();
	const urlState = useMemo(
		() => parseCommunityMarketCriteria(searchParams),
		[searchParams],
	);
	const [searchDraft, setSearchDraft] = useState(urlState.search);

	const criteria: CommunityMarketCriteria = useMemo(
		() => ({
			page: urlState.page,
			sort: `${urlState.sort},asc`,
			search: urlState.search || undefined,
			leagueId: urlState.leagueId,
			teamId: urlState.teamId,
			position: urlState.position,
			maxPrice: urlState.maxPrice,
		}),
		[urlState],
	);

	const p2pQuery = useP2PMarketQuery(criteria, enabled);
	const playerListingsQuery = usePlayerListingsQuery(urlState.player);
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

	const filters: CommunityMarketFilters = useMemo(
		() => ({
			search: urlState.search,
			leagueId: urlState.leagueId,
			teamId: urlState.teamId,
			position: urlState.position,
			maxPrice: urlState.maxPrice,
			sort: urlState.sort,
		}),
		[urlState],
	);

	const players: CommunityMarketPlayer[] = p2pQuery.data?.players ?? [];
	const totalPages = p2pQuery.data?.totalPages ?? 0;
	const totalPlayers = p2pQuery.data?.totalElements ?? 0;
	const priceRange = p2pQuery.data?.priceRange ?? { min: null, max: null };

	const quotePlayerIds = useMemo(() => {
		const ids = players.map((p) => p.id);
		if (urlState.player && !ids.includes(urlState.player)) {
			ids.push(urlState.player);
		}
		return ids;
	}, [players, urlState.player]);

	const quoteQuery = useMarketQuotesQuery(quotePlayerIds, authenticated);

	const getOfficialQuote = useCallback(
		(playerId: number): MarketQuote | null => {
			return quoteQuery.getQuoteState(playerId)?.quote ?? null;
		},
		[quoteQuery],
	);

	const selectedPlayerFromPage = useMemo(
		() =>
			players.find(
				(player: CommunityMarketPlayer) => player.id === urlState.player,
			) ?? null,
		[players, urlState.player],
	);

	const selectedPlayerFromAll = useMemo(() => {
		if (!urlState.player || !playersQuery.data?.players) return null;
		const found = playersQuery.data.players.find(
			(player) => player.id === urlState.player,
		);
		return found ? toMarketPlayer(found) : null;
	}, [playersQuery.data?.players, urlState.player]);

	const selectedPlayer: CommunityMarketPlayer | null = useMemo(() => {
		if (urlState.player === null) return null;
		const base = selectedPlayerFromPage ?? selectedPlayerFromAll;
		if (!base) return null;
		const listings = playerListingsQuery.data ?? [];
		return {
			...base,
			listings,
			listingCount:
				"listingCount" in base && typeof base.listingCount === "number"
					? base.listingCount
					: listings.length,
			availableQuantity:
				"availableQuantity" in base &&
				typeof base.availableQuantity === "number"
					? base.availableQuantity
					: listings.reduce((sum, l) => sum + l.quantity, 0),
			minimumUnitPrice:
				"minimumUnitPrice" in base && typeof base.minimumUnitPrice === "number"
					? base.minimumUnitPrice
					: (listings[0]?.unitPrice ?? 0),
		};
	}, [
		playerListingsQuery.data,
		selectedPlayerFromAll,
		selectedPlayerFromPage,
		urlState.player,
	]);

	const unfilteredSelectedPlayer = selectedPlayer;

	const availableLeagues = useMemo(() => {
		if (!playersQuery.data?.players) return [];
		const leagues = new Map<number, string>();
		for (const player of playersQuery.data.players) {
			if (player.leagueId !== null && player.leagueName !== null) {
				leagues.set(player.leagueId, player.leagueName);
			}
		}
		return [...leagues]
			.map(([id, name]) => ({ id, name }))
			.sort((a, b) => a.name.localeCompare(b.name, "es"));
	}, [playersQuery.data?.players]);

	const availableTeams = useMemo(() => {
		if (!playersQuery.data?.players) return [];
		const teams = new Map<
			number,
			{ id: number; name: string; leagueId: number; leagueName: string }
		>();
		for (const player of playersQuery.data.players) {
			if (
				player.teamId !== null &&
				player.teamName !== null &&
				player.leagueId !== null &&
				player.leagueName !== null
			) {
				teams.set(player.teamId, {
					id: player.teamId,
					name: player.teamName,
					leagueId: player.leagueId,
					leagueName: player.leagueName,
				});
			}
		}
		return [...teams.values()].sort((a, b) =>
			a.name.localeCompare(b.name, "es"),
		);
	}, [playersQuery.data?.players]);

	const availablePositions = useMemo(() => {
		if (!playersQuery.data?.players) return [];
		return sanitizePositions(
			playersQuery.data.players.map((player) => player.position),
		);
	}, [playersQuery.data?.players]);

	const isLoading =
		p2pQuery.isPending ||
		(urlState.player !== null && playerListingsQuery.isPending);
	const isUpdating =
		!isLoading && (p2pQuery.isFetching || playerListingsQuery.isFetching);
	const isError =
		p2pQuery.isError ||
		(urlState.player !== null && playerListingsQuery.isError);

	useEffect(() => {
		if (isLoading || p2pQuery.isError) return;
		const leagueValid =
			urlState.leagueId === null ||
			availableLeagues.some((item) => item.id === urlState.leagueId);
		const teamValid =
			urlState.teamId === null ||
			availableTeams.some(
				(item) =>
					item.id === urlState.teamId &&
					(urlState.leagueId === null || item.leagueId === urlState.leagueId),
			);
		const positionValid =
			urlState.position === null ||
			availablePositions.includes(urlState.position);
		if (!leagueValid || !teamValid || !positionValid)
			replaceUrlState({
				...urlState,
				leagueId: leagueValid ? urlState.leagueId : null,
				teamId: leagueValid && teamValid ? urlState.teamId : null,
				position: positionValid ? urlState.position : null,
				page: 0,
			});
	}, [
		availableLeagues,
		availablePositions,
		availableTeams,
		isLoading,
		p2pQuery.isError,
		replaceUrlState,
		urlState,
	]);

	useEffect(() => {
		if (urlState.maxPrice === null) return;
		if (priceRange.min === null || priceRange.max === null) {
			replaceUrlState({ ...urlState, maxPrice: null, page: 0 });
			return;
		}
		const normalized = Math.max(
			priceRange.min,
			Math.min(urlState.maxPrice, priceRange.max),
		);
		if (normalized !== urlState.maxPrice)
			replaceUrlState({ ...urlState, maxPrice: normalized, page: 0 });
	}, [priceRange.max, priceRange.min, replaceUrlState, urlState]);

	useEffect(() => {
		if (p2pQuery.isFetching) return;
		const lastPage = Math.max(0, totalPages - 1);
		if (urlState.page > lastPage)
			replaceUrlState({ ...urlState, page: lastPage });
	}, [p2pQuery.isFetching, replaceUrlState, totalPages, urlState]);

	const updateFilters = useCallback(
		(next: Partial<CommunityMarketFilters>) => {
			if (next.search !== undefined) {
				setSearchDraft(next.search);
				return;
			}
			const nextState = { ...urlState, ...next, page: 0 };
			if (next.leagueId !== undefined) {
				const selectedTeam = availableTeams.find(
					(team) => team.id === nextState.teamId,
				);
				if (
					selectedTeam &&
					nextState.leagueId !== null &&
					selectedTeam.leagueId !== nextState.leagueId
				)
					nextState.teamId = null;
			}
			replaceUrlState(nextState);
		},
		[availableTeams, replaceUrlState, urlState],
	);

	const clearFilters = useCallback(() => {
		setSearchDraft("");
		replaceUrlState({
			...urlState,
			search: "",
			leagueId: null,
			teamId: null,
			position: null,
			maxPrice: null,
			sort: "price",
			page: 0,
		});
	}, [replaceUrlState, urlState]);

	const clearMaxPrice = useCallback(() => {
		replaceUrlState({ ...urlState, maxPrice: null, page: 0 });
	}, [replaceUrlState, urlState]);

	const commitMaxPrice = useCallback(
		(value: number | null) =>
			replaceUrlState({ ...urlState, maxPrice: value, page: 0 }),
		[replaceUrlState, urlState],
	);

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
		if (purchaseResult && !selectedPlayer)
			replaceUrlState({ ...urlState, player: null });
		setSelectedListing(null);
		setPurchasePlayer(null);
		setPurchaseResult(null);
		setPurchaseError(null);
	}, [
		buyMutation.isPending,
		purchaseResult,
		replaceUrlState,
		selectedPlayer,
		urlState,
	]);

	const retry = useCallback(async () => {
		await Promise.all([
			p2pQuery.refetch(),
			playerListingsQuery.refetch(),
			playersQuery.refetch(),
		]);
	}, [p2pQuery, playerListingsQuery, playersQuery]);

	const confirmListing = useCallback(async () => {
		if (
			!selectedListing ||
			buyMutation.isPending ||
			submittingRef.current ||
			purchaseResult ||
			Date.parse(selectedListing.expiresAt) <= Date.now() ||
			(currentUser.data?.availableCreditsCentimes !== undefined &&
				currentUser.data.availableCreditsCentimes < selectedListing.totalAmount)
		)
			return;
		submittingRef.current = true;
		setPurchaseError(null);
		try {
			const result = await buyMutation.mutateAsync(selectedListing.listingId);
			setPurchaseResult(result);
			notifySuccess(purchasePlayer?.name ?? "Jugador", result);
			await Promise.all([
				queryClient.invalidateQueries({ queryKey: marketQueryKeys.p2pAll() }),
				queryClient.invalidateQueries({
					queryKey: marketQueryKeys.listings(result.playerId),
				}),
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
				await Promise.all([
					queryClient.invalidateQueries({
						queryKey: marketQueryKeys.p2pAll(),
					}),
					queryClient.invalidateQueries({
						queryKey: marketQueryKeys.listings(),
					}),
				]);
		} finally {
			submittingRef.current = false;
		}
	}, [
		buyMutation,
		currentUser.data?.availableCreditsCentimes,
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
		priceRange,
		commitMaxPrice,
		updateFilters,
		clearFilters,
		clearMaxPrice,
		leagues: availableLeagues,
		teams: availableTeams,
		positions: availablePositions,
		players,
		totalPlayers,
		availableCredits: currentUser.data?.availableCredits,
		availableCreditsCentimes: currentUser.data?.availableCreditsCentimes,
		page: urlState.page,
		totalPages,
		setPage: (page: number) => replaceUrlState({ ...urlState, page }),
		selectedPlayerId: urlState.player,
		selectedPlayer,
		unfilteredSelectedPlayer,
		openPlayer,
		closePlayer,
		selectedListing,
		purchasePlayer,
		openListing,
		closeListing,
		confirmListing,
		purchaseResult,
		purchaseReturnsToCatalog: Boolean(purchaseResult && !selectedPlayer),
		purchaseError,
		canConfirmListing: Boolean(
			selectedListing &&
				!purchaseResult &&
				Date.parse(selectedListing.expiresAt) > Date.now() &&
				(currentUser.data?.availableCreditsCentimes === undefined ||
					currentUser.data.availableCreditsCentimes >=
						selectedListing.totalAmount),
		),
		isBuying: buyMutation.isPending || submittingRef.current,
		isLoading,
		isUpdating,
		isError,
		errorMessage: isError
			? getErrorMessage(
					p2pQuery.error ?? playerListingsQuery.error ?? playersQuery.error,
					"No pudimos cargar las ofertas de usuarios.",
				)
			: null,
		retry,
		triggerRef,
		quoteQuery,
		getOfficialQuote,
	};
}

export type CommunityMarketState = ReturnType<typeof useCommunityMarketPage>;
