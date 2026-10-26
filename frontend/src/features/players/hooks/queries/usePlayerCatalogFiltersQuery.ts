import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { playersService } from "@/features/players/players.service";

export function usePlayerCatalogFiltersQuery() {
	return useQuery({ queryKey: playersQueryKeys.filters, queryFn: ({ signal }) => playersService.getFilters(signal), staleTime: 10 * 60 * 1000, retry: false });
}
