import { useEffect, useState } from "react";
import {
	usePlayerCatalogFiltersQuery,
	usePlayersQuery,
} from "@/features/players/hooks/queries";
import type { PlayerCatalogCriteria } from "@/features/players/types";
import { getErrorMessage } from "@/infrastructure/http";

const defaults: PlayerCatalogCriteria = {
	name: "",
	leagueId: "",
	teamId: "",
	position: "",
	nationality: "",
	sort: "name,asc",
	page: 0,
};

export function usePlayersPage() {
	const [criteria, setCriteria] = useState<PlayerCatalogCriteria>(defaults);
	const [searchText, setSearchText] = useState("");
	const query = usePlayersQuery(criteria.page, criteria);
	const filters = usePlayerCatalogFiltersQuery();
	useEffect(() => {
		const timer = window.setTimeout(
			() =>
				setCriteria((current) =>
					current.name === searchText.trim()
						? current
						: { ...current, name: searchText.trim(), page: 0 },
				),
			300,
		);
		return () => window.clearTimeout(timer);
	}, [searchText]);
	useEffect(() => {
		if (!query.data || query.isFetching || query.isError) return;
		const last = Math.max(0, query.data.totalPages - 1);
		if (criteria.page > last)
			setCriteria((current) =>
				current.page === criteria.page ? { ...current, page: last } : current,
			);
	}, [query.data, query.isFetching, query.isError, criteria.page]);
	function changeFilter(
		key: "leagueId" | "teamId" | "position" | "nationality" | "sort",
		value: string,
	) {
		if (key === "sort" && value !== "name,asc" && value !== "name,desc") return;
		setCriteria((current) => ({ ...current, [key]: value, page: 0 }));
	}
	function clearFilters() {
		setSearchText("");
		setCriteria(defaults);
	}
	function selectPage(page: number) {
		if (page >= 0 && page < (query.data?.totalPages ?? 0))
			setCriteria((current) => ({ ...current, page }));
	}
	const isLoading = query.isPending || query.isFetching;
	return {
		criteria,
		searchText,
		setSearchText,
		changeFilter,
		clearFilters,
		selectPage,
		selectedPage: criteria.page,
		playersPage: !isLoading && !query.isError ? query.data : undefined,
		isLoading,
		errorMessage: query.isError
			? getErrorMessage(
					query.error,
					"No pudimos cargar los jugadores. Vuelve a intentarlo.",
				)
			: null,
		options: filters.data,
		optionsLoading: filters.isPending,
		optionsError: filters.isError
			? getErrorMessage(
					filters.error,
					"No pudimos cargar las opciones de filtros.",
				)
			: null,
		retry: query.refetch,
		retryOptions: filters.refetch,
	};
}
