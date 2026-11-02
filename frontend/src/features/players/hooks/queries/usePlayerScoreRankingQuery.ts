import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { playersService } from "@/features/players/players.service";

export function usePlayerScoreRankingQuery(enabled = true) {
	return useQuery({
		queryKey: playersQueryKeys.scoreRanking,
		queryFn: ({ signal }) => playersService.getScoreRanking(signal),
		enabled,
		staleTime: 60_000,
		retry: 1,
	});
}
