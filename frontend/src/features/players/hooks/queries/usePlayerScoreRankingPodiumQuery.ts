import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { playersService } from "@/features/players/players.service";
import type { RankingQuery } from "@/features/players/types";

const PODIUM_QUERY: RankingQuery = { page: 0, size: 3 };

export function usePlayerScoreRankingPodiumQuery(enabled = true) {
	return useQuery({
		queryKey: playersQueryKeys.rankingPodium("PERFORMANCE"),
		queryFn: ({ signal }) =>
			playersService.getScoreRanking(PODIUM_QUERY, signal),
		enabled,
		staleTime: 60_000,
		retry: 1,
	});
}
