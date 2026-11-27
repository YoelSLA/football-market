import { useQueries } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { playersQueryKeys } from "@/features/players/constants";
import {
	toQuoteRankingItems,
	toQuoteRankingMetadata,
	toQuoteRankingPage,
	toRankingFilterOptions,
	toScoreRankingItems,
	toScoreRankingMetadata,
	toScoreRankingPage,
} from "@/features/players/players.mapper";
import { playersService } from "@/features/players/players.service";
import type {
	PlayerQuote,
	RankingFilterCriteria,
	RankingFilterOptions,
	RankingMetadata,
	RankingMode,
	RankingPage,
	RankingQuery,
	RankingSort,
} from "@/features/players/types";
import {
	usePlayerQuoteRankingPodiumQuery,
	usePlayerQuoteRankingQuery,
	usePlayerScoreRankingPodiumQuery,
	usePlayerScoreRankingQuery,
} from "../queries";

const RANKING_DEFAULT_PAGE_SIZE = 10;
const RANKING_PAGE_SIZE_OPTIONS = [10, 20, 50];

const EMPTY_FILTER_OPTIONS: RankingFilterOptions = {
	leagues: [],
	positions: [],
};

const EMPTY_PAGE: RankingPage = {
	items: [],
	page: 0,
	size: RANKING_DEFAULT_PAGE_SIZE,
	totalElements: 0,
	totalPages: 0,
	filterOptions: EMPTY_FILTER_OPTIONS,
};

export function usePlayerRankingsPage() {
	const [sort, setSort] = useState<RankingSort>("score_desc");
	const [mode, setMode] = useState<RankingMode>("PERFORMANCE");
	const [pageIndex, setPageIndex] = useState(0);
	const [pageSize, setPageSize] = useState(RANKING_DEFAULT_PAGE_SIZE);
	const [filters, setFilters] = useState<RankingFilterCriteria>({
		name: "",
		leagueId: "",
		position: "",
	});
	const [debouncedName, setDebouncedName] = useState(filters.name);

	useEffect(() => {
		const timer = window.setTimeout(() => {
			setDebouncedName(filters.name);
		}, 300);
		return () => window.clearTimeout(timer);
	}, [filters.name]);

	const hasActiveFilters =
		Boolean(filters.name.trim()) ||
		Boolean(filters.leagueId) ||
		Boolean(filters.position);

	// Parámetros efectivos de la tabla; la página interna es base cero.
	const tableQuery = useMemo<RankingQuery>(() => {
		const leagueId = filters.leagueId
			? Number.parseInt(filters.leagueId, 10)
			: Number.NaN;
		const order = sort === "score_asc" || sort === "price_asc" ? "asc" : "desc";

		return {
			page: pageIndex,
			size: pageSize,
			name: debouncedName.trim() || undefined,
			leagueId: Number.isNaN(leagueId) ? undefined : leagueId,
			position: filters.position || undefined,
			order,
		};
	}, [
		debouncedName,
		filters.leagueId,
		filters.position,
		pageIndex,
		pageSize,
		sort,
	]);

	const scoreTableQuery = usePlayerScoreRankingQuery(
		tableQuery,
		mode === "PERFORMANCE",
	);
	const quoteTableQuery = usePlayerQuoteRankingQuery(
		tableQuery,
		mode === "VALUATION",
	);
	const scorePodiumQuery = usePlayerScoreRankingPodiumQuery(
		mode === "PERFORMANCE",
	);
	const quotePodiumQuery = usePlayerQuoteRankingPodiumQuery(
		mode === "VALUATION",
	);

	const activeTableQuery =
		mode === "PERFORMANCE" ? scoreTableQuery : quoteTableQuery;
	const activePodiumQuery =
		mode === "PERFORMANCE" ? scorePodiumQuery : quotePodiumQuery;

	// El podio usa exclusivamente la consulta independiente `page=0&size=3` sin filtros.
	const rawPodiumItems = useMemo(() => {
		if (mode === "PERFORMANCE") {
			return scorePodiumQuery.data
				? toScoreRankingItems(scorePodiumQuery.data)
				: [];
		}

		return quotePodiumQuery.data
			? toQuoteRankingItems(quotePodiumQuery.data)
			: [];
	}, [mode, scorePodiumQuery.data, quotePodiumQuery.data]);

	const tablePage = useMemo<RankingPage>(() => {
		if (mode === "PERFORMANCE") {
			return scoreTableQuery.data
				? toScoreRankingPage(scoreTableQuery.data)
				: EMPTY_PAGE;
		}

		return quoteTableQuery.data
			? toQuoteRankingPage(quoteTableQuery.data)
			: EMPTY_PAGE;
	}, [mode, scoreTableQuery.data, quoteTableQuery.data]);

	// Jugadores visibles para complementar con métricas duales (Score + Cotización)
	const visiblePlayerIds = useMemo(() => {
		const ids = new Set<number>();
		for (const p of rawPodiumItems) {
			if (p.playerId) ids.add(p.playerId);
		}
		for (const p of tablePage.items) {
			if (p.playerId) ids.add(p.playerId);
		}
		return Array.from(ids);
	}, [rawPodiumItems, tablePage.items]);

	const currentQuoteQueries = useQueries({
		queries: visiblePlayerIds.map((playerId) => ({
			queryKey: playersQueryKeys.currentQuote(playerId),
			queryFn: ({ signal }: { signal: AbortSignal }) =>
				playersService.getCurrentQuote(playerId, signal),
			staleTime: 5 * 60 * 1000,
			enabled: playerId > 0,
		})),
	});

	const quotesMap = useMemo(() => {
		const map = new Map<number, PlayerQuote>();
		currentQuoteQueries.forEach((q, idx) => {
			const id = visiblePlayerIds[idx];
			if (id && q.data) {
				map.set(id, q.data);
			}
		});
		return map;
	}, [currentQuoteQueries, visiblePlayerIds]);

	const podiumItems = useMemo(() => {
		return rawPodiumItems.map((item) => {
			const quote = quotesMap.get(item.playerId);
			const complementaryScore =
				quote?.score !== null && quote?.score !== undefined
					? Number(quote.score)
					: undefined;
			return {
				...item,
				score: item.score ?? complementaryScore,
				price: item.price ?? quote?.price,
			};
		});
	}, [rawPodiumItems, quotesMap]);

	const tableItems = useMemo(() => {
		return tablePage.items.map((item) => {
			const quote = quotesMap.get(item.playerId);
			const complementaryScore =
				quote?.score !== null && quote?.score !== undefined
					? Number(quote.score)
					: undefined;
			return {
				...item,
				score: item.score ?? complementaryScore,
				price: item.price ?? quote?.price,
			};
		});
	}, [tablePage.items, quotesMap]);

	// Las opciones de filtro son globales; se usan las de la tabla o, en su defecto, las del podio.
	const filterOptions = useMemo<RankingFilterOptions>(() => {
		const options =
			(mode === "PERFORMANCE"
				? scoreTableQuery.data?.filterOptions
				: quoteTableQuery.data?.filterOptions) ??
			(mode === "PERFORMANCE"
				? scorePodiumQuery.data?.filterOptions
				: quotePodiumQuery.data?.filterOptions);

		return options ? toRankingFilterOptions(options) : EMPTY_FILTER_OPTIONS;
	}, [
		mode,
		scoreTableQuery.data?.filterOptions,
		quoteTableQuery.data?.filterOptions,
		scorePodiumQuery.data?.filterOptions,
		quotePodiumQuery.data?.filterOptions,
	]);

	const globalTotal = activePodiumQuery.data
		? activePodiumQuery.data.totalElements
		: hasActiveFilters
			? null
			: tablePage.totalElements;

	const metadata = useMemo<RankingMetadata>(() => {
		if (mode === "PERFORMANCE") {
			return toScoreRankingMetadata(
				scoreTableQuery.data ?? scorePodiumQuery.data,
				globalTotal,
			);
		}

		return toQuoteRankingMetadata(
			quotePodiumQuery.data ?? quoteTableQuery.data,
			globalTotal,
		);
	}, [
		mode,
		scoreTableQuery.data,
		scorePodiumQuery.data,
		quoteTableQuery.data,
		quotePodiumQuery.data,
		globalTotal,
	]);

	const handleModeChange = (newMode: RankingMode) => {
		if (newMode !== mode) {
			setMode(newMode);
			setSort(newMode === "PERFORMANCE" ? "score_desc" : "price_desc");
			setPageIndex(0);
		}
	};

	const handleFilterChange = (
		key: keyof RankingFilterCriteria,
		value: string,
	) => {
		if (key === "sort") {
			const newSort = value as RankingSort;
			setSort(newSort);
			if (newSort === "score_desc" || newSort === "score_asc") {
				setMode("PERFORMANCE");
			} else {
				setMode("VALUATION");
			}
			setPageIndex(0);
			return;
		}
		setFilters((prev) => ({ ...prev, [key]: value }));
		setPageIndex(0);
	};

	const handleClearFilters = () => {
		setFilters({
			name: "",
			leagueId: "",
			position: "",
		});
		setSort("score_desc");
		setMode("PERFORMANCE");
		setPageIndex(0);
	};

	const handlePageChange = (page: number) => {
		if (page >= 1 && page <= Math.max(1, tablePage.totalPages)) {
			setPageIndex(page - 1);
		}
	};

	const handlePageSizeChange = (size: number) => {
		setPageSize(size);
		setPageIndex(0);
	};

	const isLoading = activeTableQuery.isPending;
	const isError = activeTableQuery.isError;
	const isEmpty = !isLoading && !isError && tablePage.totalElements === 0;
	const currentPage = Math.min(
		tablePage.page + 1,
		Math.max(1, tablePage.totalPages),
	);

	return {
		mode,
		handleModeChange,
		sort,
		metadata,
		podiumItems,
		tableItems,
		filters: { ...filters, sort },
		handleFilterChange,
		handleClearFilters,
		filterOptions,
		hasActiveFilters,
		pagination: {
			currentPage,
			totalPages: tablePage.totalPages,
			totalItems: tablePage.totalElements,
			itemsOnPage: tablePage.items.length,
			pageSize: tablePage.size || pageSize,
			pageSizeOptions: RANKING_PAGE_SIZE_OPTIONS,
			onPageChange: handlePageChange,
			onPageSizeChange: handlePageSizeChange,
		},
		status: {
			isLoading,
			isFetching: activeTableQuery.isFetching || activePodiumQuery.isFetching,
			isError,
			error: activeTableQuery.error,
			retry: () => {
				activeTableQuery.refetch();
				activePodiumQuery.refetch();
			},
			isGlobalEmpty: isEmpty && !hasActiveFilters,
			isFilterEmpty: isEmpty && hasActiveFilters,
		},
	};
}
