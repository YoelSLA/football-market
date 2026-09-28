import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "../../constants";
import { playersService } from "../../players.service";

export const PLAYERS_PAGE_SIZE = 12;

export function usePlayersQuery(page: number) {
	return useQuery({
		queryKey: playersQueryKeys.page(page, PLAYERS_PAGE_SIZE),
		queryFn: ({ signal }) =>
			playersService.getPage(page, PLAYERS_PAGE_SIZE, signal),
	});
}
