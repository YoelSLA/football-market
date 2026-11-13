import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { playersService } from "@/features/players/players.service";
import type { RankingQuery } from "@/features/players/types";

export function usePlayerQuoteRankingQuery(
	query: RankingQuery,
	enabled = true,
) {
	return useQuery({
		queryKey: playersQueryKeys.rankingTable("VALUATION", query),
		queryFn: ({ signal }) => playersService.getQuoteRanking(query, signal),
		enabled,
		staleTime: 60_000,
		retry: 1,
	});
}
