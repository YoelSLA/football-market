import { useMemo } from "react";
import { useMarketTrendsQuery } from "./useMarketTrendsQuery";

/**
 * Hook selector que deriva un Set con los IDs de los jugadores en el Top 5
 * de demanda caliente a partir de la consulta reactiva de tendencias (SPEC-029).
 */
export function useHotPlayerIds(): {
	hotPlayerIds: Set<number>;
	isLoading: boolean;
	isError: boolean;
} {
	const { data, isLoading, isError } = useMarketTrendsQuery();

	const hotPlayerIds = useMemo(() => {
		if (!data?.topHotPlayers?.length) return new Set<number>();
		return new Set<number>(data.topHotPlayers.map((player) => player.playerId));
	}, [data?.topHotPlayers]);

	return { hotPlayerIds, isLoading, isError };
}
