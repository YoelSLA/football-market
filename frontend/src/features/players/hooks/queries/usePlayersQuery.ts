import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { playersService } from "@/features/players/players.service";

export const PLAYERS_PAGE_SIZE = 12;

export function usePlayersQuery(page: number, criteria?: import("@/features/players/types").PlayerCatalogCriteria) {
	return useQuery({
		queryKey: criteria ? playersQueryKeys.catalog(criteria, PLAYERS_PAGE_SIZE) : playersQueryKeys.page(page, PLAYERS_PAGE_SIZE),
		queryFn: ({ signal }) =>
			playersService.getPage(page, PLAYERS_PAGE_SIZE, signal, criteria),
		retry: false,
	});
}
