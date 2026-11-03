import { useQueries } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { playersService } from "@/features/players/players.service";
import type { Player, PlayersPage } from "@/features/players/types";
import { PLAYERS_PAGE_SIZE, usePlayersQuery } from "./usePlayersQuery";

export function useAllPlayersQuery() {
	const firstQuery = usePlayersQuery(0);
	const pageCount = firstQuery.data?.totalPages ?? 1;
	const pages = useQueries({
		queries: Array.from({ length: Math.max(0, pageCount - 1) }, (_, index) => {
			const page = index + 1;
			return {
				queryKey: playersQueryKeys.page(page, PLAYERS_PAGE_SIZE),
				queryFn: ({ signal }: { signal: AbortSignal }) =>
					playersService.getPage(page, PLAYERS_PAGE_SIZE, signal),
			};
		}),
	});
	const players: Player[] = [
		...(firstQuery.data?.players ?? []),
		...pages.flatMap((query) => query.data?.players ?? []),
	];
	const isPending = firstQuery.isPending;
	const isFetching =
		firstQuery.isFetching || pages.some((query) => query.isFetching);
	const isError = firstQuery.isError || pages.some((query) => query.isError);
	const refetch = async () =>
		Promise.all([
			firstQuery.refetch(),
			...pages.map((query) => query.refetch()),
		]);
	return {
		data: firstQuery.data
			? ({ ...firstQuery.data, players } as PlayersPage)
			: undefined,
		isPending,
		isFetching,
		isError,
		error: firstQuery.error ?? pages.find((query) => query.error)?.error,
		refetch,
	};
}
