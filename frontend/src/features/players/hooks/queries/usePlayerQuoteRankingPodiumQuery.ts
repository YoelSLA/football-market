import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { playersService } from "@/features/players/players.service";
import type { RankingQuery } from "@/features/players/types";

const PODIUM_QUERY: RankingQuery = { page: 0, size: 3 };

export function usePlayerQuoteRankingPodiumQuery(enabled = true) {
	return useQuery({
		queryKey: playersQueryKeys.rankingPodium("VALUATION"),
		queryFn: ({ signal }) =>
			playersService.getQuoteRanking(PODIUM_QUERY, signal),
		enabled,
		staleTime: 60_000,
		retry: 1,
	});
}
