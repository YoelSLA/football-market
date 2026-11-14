import { useCallback } from "react";
import {
	usePlayerDetailQuery,
	usePlayerQuotesQuery,
	usePlayerStatisticsQuery,
} from "@/features/players/hooks/queries";
import type {
	PlayerDetailState,
	QueryState,
	StatisticsTab,
} from "@/features/players/types";
import { parsePlayerId } from "@/features/players/utils";
import { getErrorCode, getErrorMessage } from "@/infrastructure/http";
import { usePlayerDetailControls } from "./usePlayerDetailControls";

export type { PlayerDetailState, StatisticsTab };

export function usePlayerDetailPage(rawId: string | undefined) {
	const playerId = parsePlayerId(rawId);
	const invalid = playerId === null;

	const controls = usePlayerDetailControls(playerId);

	const detail = usePlayerDetailQuery(playerId ?? 0, playerId !== null);
	const statistics = usePlayerStatisticsQuery(
		playerId ?? 0,
		null,
		detail.isSuccess,
	);
	const quotes = usePlayerQuotesQuery(playerId, detail.isSuccess);

	const visibleId = playerId;
	const isCurrent = detail.data?.id === visibleId;
	const playerNotFound =
		getErrorCode(detail.error) === "PLAYER_NOT_FOUND" ||
		getErrorCode(statistics.error) === "PLAYER_NOT_FOUND";
	const player = isCurrent && !playerNotFound ? detail.data : undefined;
	const retry = useCallback(() => void detail.refetch(), [detail]);
	const retryStatistics = useCallback(
		() => void statistics.refetch(),
		[statistics],
	);
	const retryQuotes = useCallback(() => void quotes.refetch(), [quotes]);

	const detailState: PlayerDetailState = invalid
		? "invalid-id"
		: player
			? "ready"
			: detail.isPending
				? "loading"
				: playerNotFound
					? "not-found"
					: "error";

	const statisticsState: QueryState = statistics.isPending
		? "loading"
		: statistics.isError
			? "error"
			: "ready";

	const statisticsErrorMessage =
		statistics.isError && statistics.error
			? getErrorMessage(statistics.error, "Error al cargar estadísticas")
			: null;

	const quotesState: QueryState = quotes.isPending
		? "loading"
		: quotes.isError
			? "error"
			: "ready";

	const quotesErrorMessage =
		quotes.isError && quotes.error
			? getErrorMessage(quotes.error, "Error al cargar cotizaciones")
			: null;

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
		statisticsState,
		statisticsErrorMessage,
		quotes: quotes.data?.playerId === playerId ? quotes.data : undefined,
		currentQuote:
			quotes.data?.playerId === playerId
				? (quotes.data.currentQuote ?? null)
				: null,
		quotesState,
		quotesErrorMessage,
		errorMessage,
		retry,
		retryStatistics,
		retryQuotes,
		...controls,
	};
}
