import { useCallback, useState } from "react";
import {
	usePlayerDetailQuery,
	usePlayerStatisticsQuery,
} from "@/features/players/hooks/queries";
import { getErrorCode, getErrorMessage } from "@/infrastructure/http";

export type PlayerDetailState =
	| "loading"
	| "invalid-id"
	| "not-found"
	| "error"
	| "ready";

export type StatisticsTab = "summary" | "catalog";

function parsePlayerId(rawId: string | undefined): number | null {
	if (!rawId || !/^\d+$/.test(rawId)) return null;

	const parsed = Number(rawId);

	return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

export function usePlayerDetailPage(rawId: string | undefined) {
	const playerId = parsePlayerId(rawId);
	const invalid = playerId === null;

	const initial = {
		id: playerId,
		tab: "summary" as StatisticsTab,
		search: "",
		category: "all",
		unit: "all",
	};
	const [controls, setControls] = useState(initial);
	const currentControls = controls.id === playerId ? controls : initial;
	const setActiveTab = (tab: StatisticsTab) =>
		setControls({ ...currentControls, tab });
	const setSearch = (search: string) =>
		setControls({ ...currentControls, search });
	const setCategory = (category: string) =>
		setControls({ ...currentControls, category });
	const setUnit = (unit: string) => setControls({ ...currentControls, unit });
	const clearFilters = () =>
		setControls({
			...currentControls,
			search: "",
			category: "all",
			unit: "all",
		});

	const detail = usePlayerDetailQuery(playerId ?? 0, playerId !== null);
	const statistics = usePlayerStatisticsQuery(
		playerId ?? 0,
		null,
		detail.isSuccess,
	);

	const visibleId = playerId;
	const isCurrent = detail.data?.id === visibleId;
	const player = isCurrent ? detail.data : undefined;
	const retry = useCallback(() => void detail.refetch(), [detail]);
	const retryStatistics = useCallback(
		() => void statistics.refetch(),
		[statistics],
	);

	const detailState: PlayerDetailState = invalid
		? "invalid-id"
		: player
			? "ready"
			: detail.isPending
				? "loading"
				: getErrorCode(detail.error) === "PLAYER_NOT_FOUND"
					? "not-found"
					: "error";

	const errorMessage =
		detailState === "error"
			? getErrorMessage(
					detail.error,
					"No pudimos consultar el jugador. Vuelve a intentarlo.",
				)
			: null;

	return {
		playerId,
		invalid,
		detailState,
		player,
		statistics:
			statistics.data?.playerId === playerId ? statistics.data : undefined,
		statisticsState: statistics.isPending
			? "loading"
			: statistics.isError
				? "error"
				: "ready",
		statisticsErrorMessage:
			statistics.isError && statistics.error
				? getErrorMessage(statistics.error, "Error al cargar estadísticas")
				: null,
		errorMessage,
		retry,
		retryStatistics,
		activeTab: currentControls.tab,
		setActiveTab,
		search: currentControls.search,
		setSearch,
		category: currentControls.category,
		setCategory,
		unit: currentControls.unit,
		setUnit,
		clearFilters,
	};
}
