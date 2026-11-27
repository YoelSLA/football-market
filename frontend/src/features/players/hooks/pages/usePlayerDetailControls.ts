import { useState } from "react";
import type { StatisticsTab } from "@/features/players/types";

export interface PlayerDetailControls {
	activeTab: StatisticsTab;
	setActiveTab: (tab: StatisticsTab) => void;
	search: string;
	setSearch: (search: string) => void;
	category: string;
	setCategory: (category: string) => void;
	clearFilters: () => void;
}

/**
 * Gestiona el estado de navegación por pestañas y filtros del catálogo
 * dentro de la página de detalle del jugador, reinicializándose si cambia el jugador.
 */
export function usePlayerDetailControls(
	playerId: number | null,
): PlayerDetailControls {
	const initial = {
		id: playerId,
		tab: "quotes" as StatisticsTab,
		search: "",
		category: "all",
	};

	const [controls, setControls] = useState(initial);
	const currentControls = controls.id === playerId ? controls : initial;

	const setActiveTab = (tab: StatisticsTab) =>
		setControls({ ...currentControls, tab });

	const setSearch = (search: string) =>
		setControls({ ...currentControls, search });

	const setCategory = (category: string) =>
		setControls({ ...currentControls, category });

	const clearFilters = () =>
		setControls({
			...currentControls,
			search: "",
			category: "all",
		});

	return {
		activeTab: currentControls.tab,
		setActiveTab,
		search: currentControls.search,
		setSearch,
		category: currentControls.category,
		setCategory,
		clearFilters,
	};
}
