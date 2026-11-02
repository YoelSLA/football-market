import { useMemo, useState } from "react";
import {
	toQuoteRankingItems,
	toQuoteRankingMetadata,
	toScoreRankingItems,
	toScoreRankingMetadata,
} from "@/features/players/players.mapper";
import type {
	Player,
	PlayerRankingItem,
	RankingFilterCriteria,
	RankingMetadata,
	RankingMode,
} from "@/features/players/types";
import {
	useAllPlayersQuery,
	usePlayerQuoteRankingQuery,
	usePlayerScoreRankingQuery,
} from "../queries";

const RANKING_PAGE_SIZE = 8;

export function usePlayerRankingsPage() {
	const [mode, setMode] = useState<RankingMode>("PERFORMANCE");
	const [currentPage, setCurrentPage] = useState(1);
	const [filters, setFilters] = useState<RankingFilterCriteria>({
		searchTerm: "",
		selectedLeague: "",
		selectedPosition: "",
	});

	// Consultas a backend
	const scoreQuery = usePlayerScoreRankingQuery(mode === "PERFORMANCE");
	const quoteQuery = usePlayerQuoteRankingQuery(mode === "VALUATION");
	const catalogQuery = useAllPlayersQuery();

	// Catálogo de jugadores indexado en mapa O(1)
	const playersMap = useMemo(() => {
		const map = new Map<number, Player>();
		const players = catalogQuery.data?.players ?? [];
		for (const player of players) {
			map.set(player.id, player);
		}
		return map;
	}, [catalogQuery.data?.players]);

	// Mapeo e hidratación del ranking completo
	const { allRankedItems, metadata } = useMemo<{
		allRankedItems: PlayerRankingItem[];
		metadata: RankingMetadata;
	}>(() => {
		if (mode === "PERFORMANCE") {
			const items = scoreQuery.data
				? toScoreRankingItems(scoreQuery.data, playersMap)
				: [];
			const meta = toScoreRankingMetadata(scoreQuery.data);
			return { allRankedItems: items, metadata: meta };
		}

		const items = quoteQuery.data
			? toQuoteRankingItems(quoteQuery.data, playersMap)
			: [];
		const meta = toQuoteRankingMetadata(quoteQuery.data);
		return { allRankedItems: items, metadata: meta };
	}, [mode, scoreQuery.data, quoteQuery.data, playersMap]);

	// Podio Top 3 fijo del ranking global activo (no afectado por filtros de tabla)
	const podiumPlayers = useMemo(() => {
		return allRankedItems.slice(0, 3);
	}, [allRankedItems]);

	// Ligas disponibles para el selector de filtros
	const availableLeagues = useMemo(() => {
		const leagues = new Set<string>();
		for (const item of allRankedItems) {
			if (item.leagueName && item.leagueName !== "Sin liga") {
				leagues.add(item.leagueName);
			}
		}
		return Array.from(leagues).sort();
	}, [allRankedItems]);

	// Filtrado en cliente de la tabla
	const filteredRankedItems = useMemo(() => {
		let result = allRankedItems;

		const search = filters.searchTerm
			.trim()
			.toLowerCase()
			.normalize("NFD")
			.replace(/[\u0300-\u036f]/g, "");

		if (search) {
			result = result.filter((item) =>
				item.playerName
					.toLowerCase()
					.normalize("NFD")
					.replace(/[\u0300-\u036f]/g, "")
					.includes(search),
			);
		}

		if (filters.selectedLeague) {
			result = result.filter(
				(item) => item.leagueName === filters.selectedLeague,
			);
		}

		if (filters.selectedPosition) {
			result = result.filter(
				(item) => item.tacticalPosition === filters.selectedPosition,
			);
		}

		return result;
	}, [allRankedItems, filters]);

	// Paginación
	const totalItems = filteredRankedItems.length;
	const totalPages = Math.max(1, Math.ceil(totalItems / RANKING_PAGE_SIZE));
	const safePage = Math.min(currentPage, totalPages);

	const paginatedItems = useMemo(() => {
		const start = (safePage - 1) * RANKING_PAGE_SIZE;
		return filteredRankedItems.slice(start, start + RANKING_PAGE_SIZE);
	}, [filteredRankedItems, safePage]);

	// Manejadores de cambios
	const handleModeChange = (newMode: RankingMode) => {
		if (newMode !== mode) {
			setMode(newMode);
			setCurrentPage(1);
		}
	};

	const handleFilterChange = (
		key: keyof RankingFilterCriteria,
		value: string,
	) => {
		setFilters((prev) => ({ ...prev, [key]: value }));
		setCurrentPage(1);
	};

	const handleClearFilters = () => {
		setFilters({
			searchTerm: "",
			selectedLeague: "",
			selectedPosition: "",
		});
		setCurrentPage(1);
	};

	const handlePageChange = (newPage: number) => {
		if (newPage >= 1 && newPage <= totalPages) {
			setCurrentPage(newPage);
		}
	};

	// Estados asíncronos y reintento
	const activeQuery = mode === "PERFORMANCE" ? scoreQuery : quoteQuery;
	const isLoading = activeQuery.isPending;
	const isFetching = activeQuery.isFetching || catalogQuery.isFetching;
	const isError = activeQuery.isError;
	const error = activeQuery.error;

	const handleRetry = () => {
		activeQuery.refetch();
		if (catalogQuery.isError) {
			catalogQuery.refetch();
		}
	};

	return {
		mode,
		handleModeChange,
		metadata,
		podiumPlayers,
		tableItems: paginatedItems,
		allRankedItems,
		filteredCount: filteredRankedItems.length,
		totalCount: allRankedItems.length,
		filters,
		handleFilterChange,
		handleClearFilters,
		availableLeagues,
		pagination: {
			currentPage: safePage,
			totalPages,
			totalItems,
			onPageChange: handlePageChange,
		},
		status: {
			isLoading,
			isFetching,
			isError,
			error,
			retry: handleRetry,
		},
	};
}
