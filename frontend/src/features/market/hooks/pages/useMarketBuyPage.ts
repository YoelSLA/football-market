import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { authQueryKeys, useCurrentUserQuery } from "@/features/auth";
import {
	MARKET_SEARCH_DEBOUNCE_MS,
	marketQueryKeys,
	OFFICIAL_MARKET_DEFAULT_PAGE_SIZE,
} from "@/features/market/constants";
import { useBuyQuantityForm } from "@/features/market/form";
import { useBuyTokensMutation } from "@/features/market/hooks/mutations";
import { useOfficialMarketQuery } from "@/features/market/hooks/queries";
import { useMarketToast } from "@/features/market/hooks/useMarketToast";
import type {
	BuyEstimation,
	BuyResult,
	OfficialMarketCriteria,
	OfficialMarketPlayer,
	OfficialMarketSort,
} from "@/features/market/types";
import {
	calculateEstimation,
	parseOfficialMarketCriteria,
	serializeOfficialMarketCriteria,
} from "@/features/market/utils";
import { usePlayerCatalogFiltersQuery } from "@/features/players";
import { portfolioQueryKeys } from "@/features/portfolio";
import { getErrorCode, getErrorMessage } from "@/infrastructure/http";
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

export function useMarketBuyPage() {
	const queryClient = useQueryClient();
	const { notifySuccess, notifyError } = useMarketToast();
	const [searchParams, setSearchParams] = useSearchParams();
	const criteria = useMemo(
		() => parseOfficialMarketCriteria(searchParams),
		[searchParams],
	);
	const [searchDraft, setSearchDraft] = useState(criteria.name);
	const authenticated = useAuthStore(
		(state) =>
			state.status === AuthStatus.AUTHENTICATED && state.session !== null,
	);
	const revision = useAuthStore((state) => state.revision);
	const currentUser = useCurrentUserQuery(true, revision);
	const filterOptions = usePlayerCatalogFiltersQuery();
	const marketQuery = useOfficialMarketQuery(criteria);
	const [selectedPlayer, setSelectedPlayer] =
		useState<OfficialMarketPlayer | null>(null);
	const [purchaseResult, setPurchaseResult] = useState<BuyResult | null>(null);
	const [errorMessage, setErrorMessage] = useState<string | null>(null);
	const [isSuccess, setIsSuccess] = useState(false);
	const triggerRef = useRef<HTMLElement | null>(null);
	const initialFocusRef = useRef<HTMLInputElement | null>(null);
	const isSubmittingRef = useRef(false);
	const autoCloseTimeoutRef = useRef<number | null>(null);
	const form = useBuyQuantityForm();
	const buyMutation = useBuyTokensMutation();

	const replaceCriteria = useCallback(
		(next: OfficialMarketCriteria) => {
			setSearchParams(serializeOfficialMarketCriteria(next), { replace: true });
		},
		[setSearchParams],
	);

	useEffect(() => {
		if (!searchParams.has("hasQuote") && !searchParams.has("size")) return;
		const nextParams = new URLSearchParams(searchParams);
		nextParams.delete("hasQuote");
		nextParams.delete("size");
		setSearchParams(nextParams, { replace: true });
	}, [searchParams, setSearchParams]);

	useEffect(() => setSearchDraft(criteria.name), [criteria.name]);
	useEffect(() => {
		if (searchDraft.trim() === criteria.name) return;
		const timeout = window.setTimeout(() => {
			replaceCriteria({ ...criteria, name: searchDraft.trim(), page: 0 });
		}, MARKET_SEARCH_DEBOUNCE_MS);
		return () => window.clearTimeout(timeout);
	}, [criteria, replaceCriteria, searchDraft]);

	useEffect(() => {
		const range = marketQuery.data?.priceRange;
		if (range?.min === null || range?.max === null) {
			if (criteria.minPrice !== null || criteria.maxPrice !== null)
				replaceCriteria({
					...criteria,
					minPrice: null,
					maxPrice: null,
					page: 0,
				});
			return;
		}
		if (!range) return;
		const minPrice = Math.max(range.min, criteria.minPrice ?? range.min);
		const maxPrice = Math.min(range.max, criteria.maxPrice ?? range.max);
		const normalizedMin = Math.min(minPrice, maxPrice);
		if (
			(criteria.minPrice !== null && criteria.minPrice !== normalizedMin) ||
			(criteria.maxPrice !== null && criteria.maxPrice !== maxPrice)
		)
			replaceCriteria({
				...criteria,
				minPrice: criteria.minPrice === null ? null : normalizedMin,
				maxPrice: criteria.maxPrice === null ? null : maxPrice,
				page: 0,
			});
	}, [criteria, marketQuery.data?.priceRange, replaceCriteria]);

	useEffect(() => {
		if (!filterOptions.data) return;
		const leagueValid =
			criteria.leagueId === null ||
			filterOptions.data.leagues.some((item) => item.id === criteria.leagueId);
		const teamValid =
			criteria.teamId === null ||
			filterOptions.data.teams.some(
				(item) =>
					item.id === criteria.teamId &&
					(criteria.leagueId === null || item.leagueId === criteria.leagueId),
			);
		const positionValid =
			criteria.position === null ||
			filterOptions.data.positions.includes(criteria.position);
		const nationalityValid =
			criteria.nationality === null ||
			filterOptions.data.nationalities.includes(criteria.nationality);
		if (!leagueValid || !teamValid || !positionValid || !nationalityValid)
			replaceCriteria({
				...criteria,
				leagueId: leagueValid ? criteria.leagueId : null,
				teamId: leagueValid && teamValid ? criteria.teamId : null,
				position: positionValid ? criteria.position : null,
				nationality: nationalityValid ? criteria.nationality : null,
				page: 0,
			});
	}, [criteria, filterOptions.data, replaceCriteria]);

	useEffect(() => {
		if (!marketQuery.data || marketQuery.isFetching) return;
		const lastPage = Math.max(0, marketQuery.data.totalPages - 1);
		if (criteria.page > lastPage)
			replaceCriteria({ ...criteria, page: lastPage });
	}, [criteria, marketQuery.data, marketQuery.isFetching, replaceCriteria]);

	useEffect(() => {
		if (!selectedPlayer || !marketQuery.data || isSuccess) return;
		const current = marketQuery.data.players.find(
			(player) => player.id === selectedPlayer.id,
		);
		if (!current?.quote) setSelectedPlayer(null);
		else setSelectedPlayer(current);
	}, [isSuccess, marketQuery.data, selectedPlayer]);

	useEffect(
		() => () => {
			if (autoCloseTimeoutRef.current !== null)
				window.clearTimeout(autoCloseTimeoutRef.current);
		},
		[],
	);

	const changeFilter = useCallback(
		(
			key: "leagueId" | "teamId" | "position" | "nationality" | "sort",
			value: string,
		) => {
			const next = { ...criteria, page: 0 };
			if (key === "leagueId") {
				next.leagueId = value === "all" ? null : Number(value);
				const selectedTeam = filterOptions.data?.teams.find(
					(team) => team.id === next.teamId,
				);
				if (selectedTeam && selectedTeam.leagueId !== next.leagueId)
					next.teamId = null;
			} else if (key === "teamId")
				next.teamId = value === "all" ? null : Number(value);
			else if (key === "position")
				next.position = value === "all" ? null : value;
			else if (key === "nationality")
				next.nationality = value === "all" ? null : value;
			else next.sort = value as OfficialMarketSort;
			replaceCriteria(next);
		},
		[criteria, filterOptions.data?.teams, replaceCriteria],
	);

	const clearFilters = useCallback(() => {
		setSearchDraft("");
		setSearchParams(new URLSearchParams(), { replace: true });
	}, [setSearchParams]);
	const commitPriceRange = useCallback(
		(value: readonly [number, number]) => {
			const range = marketQuery.data?.priceRange;
			if (range?.min === null || range?.max === null || !range) return;
			replaceCriteria({
				...criteria,
				minPrice: value[0] === range.min ? null : value[0],
				maxPrice: value[1] === range.max ? null : value[1],
				page: 0,
			});
		},
		[criteria, marketQuery.data?.priceRange, replaceCriteria],
	);

	const openBuyModal = useCallback(
		(player: OfficialMarketPlayer, triggerEl: HTMLElement | null) => {
			if (!player.quote) return;
			triggerRef.current = triggerEl;
			setSelectedPlayer(player);
			setPurchaseResult(null);
			setErrorMessage(null);
			setIsSuccess(false);
			isSubmittingRef.current = false;
			form.resetQuantity();
		},
		[form],
	);

	const closeBuyModal = useCallback(() => {
		if (buyMutation.isPending || isSubmittingRef.current) return;
		if (autoCloseTimeoutRef.current !== null)
			window.clearTimeout(autoCloseTimeoutRef.current);
		setSelectedPlayer(null);
		setPurchaseResult(null);
		setErrorMessage(null);
		setIsSuccess(false);
		form.resetQuantity();
	}, [buyMutation.isPending, form]);

	const selectedQuote = selectedPlayer?.quote ?? null;
	const estimation: BuyEstimation | null = useMemo(
		() =>
			selectedQuote && form.validatedQuantity
				? calculateEstimation(form.validatedQuantity, selectedQuote.price)
				: null,
		[selectedQuote, form.validatedQuantity],
	);

	const handleConfirmBuy = useCallback(async () => {
		if (
			!authenticated ||
			!selectedPlayer?.quote ||
			!form.validatedQuantity ||
			buyMutation.isPending ||
			isSubmittingRef.current ||
			isSuccess
		)
			return;
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
			]);
			notifySuccess(selectedPlayer.name, result);
			setPurchaseResult(result);
			setIsSuccess(true);
			autoCloseTimeoutRef.current = window.setTimeout(closeBuyModal, 2000);
		} catch (error: unknown) {
			const message = mapErrorCodeToMessage(error);
			setErrorMessage(message);
			notifyError(message);
		} finally {
			isSubmittingRef.current = false;
		}
	}, [
		authenticated,
		buyMutation,
		closeBuyModal,
		form.validatedQuantity,
		isSuccess,
		notifyError,
		notifySuccess,
		queryClient,
		selectedPlayer,
	]);

	const isBusy = buyMutation.isPending || isSubmittingRef.current;
	const canConfirm = Boolean(
		authenticated &&
			selectedPlayer?.quote &&
			form.validatedQuantity &&
			!isBusy &&
			!isSuccess,
	);

	return {
		criteria,
		searchDraft,
		setSearchDraft,
		priceRange: marketQuery.data?.priceRange ?? { min: null, max: null },
		commitPriceRange,
		changeFilter,
		clearFilters,
		filterOptions: filterOptions.data,
		filterOptionsLoading: filterOptions.isPending,
		filterOptionsError: filterOptions.isError,
		players: marketQuery.data?.players ?? [],
		totalPages: marketQuery.data?.totalPages ?? 0,
		totalPlayers: marketQuery.data?.totalElements,
		pageSize: marketQuery.data?.size ?? OFFICIAL_MARKET_DEFAULT_PAGE_SIZE,
		isLoadingPlayers: marketQuery.isPending,
		isUpdatingPlayers: marketQuery.isFetching && !marketQuery.isPending,
		isPlayersError: marketQuery.isError,
		playersErrorMessage: marketQuery.isError
			? getErrorMessage(
					marketQuery.error,
					"No se pudo cargar el Mercado Oficial.",
				)
			: null,
		retryPlayers: async () => {
			await Promise.all([marketQuery.refetch(), filterOptions.refetch()]);
		},
		setCurrentPage: (page: number) => replaceCriteria({ ...criteria, page }),
		availableCredits: currentUser.data?.availableCredits,
		isModalOpen: Boolean(selectedPlayer),
		selectedPlayer,
		selectedQuote,
		estimation,
		isSuccess,
		purchaseResult,
		errorMessage,
		isBusy,
		canConfirm,
		confirmationBlockReason: !authenticated
			? "Iniciá sesión para comprar."
			: !selectedPlayer?.quote
				? "La cotización no está disponible."
				: !form.validatedQuantity
					? "Ingresá una cantidad entera entre 1 y 100."
					: isBusy
						? "La compra está en proceso."
						: isSuccess
							? "La compra ya fue confirmada."
							: null,
		triggerRef,
		initialFocusRef,
		form,
		openBuyModal,
		closeBuyModal,
		handleConfirmBuy,
	};
}

export type MarketBuyState = ReturnType<typeof useMarketBuyPage>;
