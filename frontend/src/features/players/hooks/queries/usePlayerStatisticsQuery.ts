import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { playersService } from "@/features/players/players.service";

export function usePlayerStatisticsQuery(
	id: number,
	seasonKey: string | null,
	enabled = true,
) {
	return useQuery({
		queryKey: playersQueryKeys.statistics(id, seasonKey),
		queryFn: ({ signal }) => playersService.getStatistics(id, seasonKey, signal),
		enabled: enabled && Number.isInteger(id) && id > 0,
		retry: false,
	});
}