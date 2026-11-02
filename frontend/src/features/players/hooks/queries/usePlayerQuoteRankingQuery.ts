import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { playersService } from "@/features/players/players.service";

export function usePlayerQuoteRankingQuery(enabled = true) {
	return useQuery({
		queryKey: playersQueryKeys.quoteRanking,
		queryFn: ({ signal }) => playersService.getQuoteRanking(signal),
		enabled,
		staleTime: 60_000,
		retry: 1,
	});
}
