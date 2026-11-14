import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { playersService } from "@/features/players/players.service";
import type { RankingQuery } from "@/features/players/types";

export function usePlayerScoreRankingQuery(
	query: RankingQuery,
	enabled = true,
) {
	return useQuery({
		queryKey: playersQueryKeys.rankingTable("PERFORMANCE", query),
		queryFn: ({ signal }) => playersService.getScoreRanking(query, signal),
		enabled,
		staleTime: 60_000,
		retry: 1,
	});
}
