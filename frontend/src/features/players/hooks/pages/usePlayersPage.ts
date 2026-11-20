import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
	usePlayerCatalogFiltersQuery,
	usePlayersQuery,
} from "@/features/players/hooks/queries";
import type { PlayerCatalogCriteria } from "@/features/players/types";
import {
	parseCatalogSearchParams,
	serializeCatalogCriteria,
} from "@/features/players/utils";
import { getErrorMessage } from "@/infrastructure/http";

export function usePlayersPage() {
	const [searchParams, setSearchParams] = useSearchParams();

	// useSearchParams es la única fuente de verdad para los criterios del catálogo
	const criteria = useMemo(
		() => parseCatalogSearchParams(searchParams),
		[searchParams],
	);

	// searchText local únicamente para gestionar el input y el debounce
	const [searchText, setSearchText] = useState(criteria.name);

	// Sincronizar searchText si la URL cambia externamente (ej. navegación Atrás/Adelante)
	useEffect(() => {
		setSearchText(criteria.name);
	}, [criteria.name]);

	const query = usePlayersQuery(criteria.page, criteria);
	const filters = usePlayerCatalogFiltersQuery();

	// Debounce de búsqueda hacia la URL, cancelable para evitar condiciones de carrera
	useEffect(() => {
		if (searchText.trim() === criteria.name) return;

		const timer = window.setTimeout(() => {
			const nextCriteria: PlayerCatalogCriteria = {
				...criteria,
				name: searchText.trim(),
				page: 0,
			};
			const nextParams = serializeCatalogCriteria(nextCriteria);
			setSearchParams(nextParams, { replace: true });
		}, 300);

		return () => window.clearTimeout(timer);
	}, [searchText, criteria, setSearchParams]);

	// Ajustar página si excede el total tras filtrado
	useEffect(() => {
		if (!query.data || query.isFetching || query.isError) return;
		const last = Math.max(0, query.data.totalPages - 1);
		if (criteria.page > last) {
			const nextParams = serializeCatalogCriteria({
				...criteria,
				page: last,
			});
			setSearchParams(nextParams, { replace: true });
		}
	}, [query.data, query.isFetching, query.isError, criteria, setSearchParams]);

	function changeFilter(
		key: "leagueId" | "teamId" | "position" | "nationality" | "sort",
		value: string,
	) {
		if (key === "sort" && value !== "name,asc" && value !== "name,desc") return;

		const nextCriteria: PlayerCatalogCriteria = {
			...criteria,
			[key]: value,
			page: 0,
		};
		const nextParams = serializeCatalogCriteria(nextCriteria);
		setSearchParams(nextParams, { replace: true });
	}

	function clearFilters() {
		setSearchText("");
		setSearchParams(new URLSearchParams(), { replace: true });
	}

	function selectPage(page: number) {
		if (page >= 0 && page < (query.data?.totalPages ?? 0)) {
			const nextCriteria: PlayerCatalogCriteria = {
				...criteria,
				page,
			};
			const nextParams = serializeCatalogCriteria(nextCriteria);
			setSearchParams(nextParams, { replace: true });
		}
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
		errorMessage: query.error
			? getErrorMessage(
					query.error,
					"No pudimos cargar los jugadores. Vuelve a intentarlo.",
				)
			: null,
		options: filters.data,
		optionsLoading: filters.isPending,
		optionsError: filters.error
			? getErrorMessage(
					filters.error,
					"No pudimos cargar las opciones de filtros.",
				)
			: null,
		retry: query.refetch,
		retryOptions: filters.refetch,
	};
}
