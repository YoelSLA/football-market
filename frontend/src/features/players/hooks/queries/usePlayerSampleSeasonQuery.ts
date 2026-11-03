import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { getPlayerSampleSeason } from "@/features/players/demo";
import type { PlayerSampleSeason } from "@/features/players/types";

/**
 * Estadísticas de demostración de un jugador en la temporada fija. Solo se consultan
 * cuando la identidad real ya está resuelta.
 */
export function usePlayerSampleSeasonQuery(
	playerId: number | undefined,
	enabled = true,
) {
	return useQuery<PlayerSampleSeason, Error>({
		queryKey: playersQueryKeys.sampleSeason(playerId ?? 0),
		queryFn: () => getPlayerSampleSeason(playerId as number),
		enabled: enabled && typeof playerId === "number",
		staleTime: Number.POSITIVE_INFINITY,
		gcTime: Number.POSITIVE_INFINITY,
		retry: false,
	});
}
