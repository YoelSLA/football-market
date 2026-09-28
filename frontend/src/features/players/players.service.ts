import { http } from "@/infrastructure/http";
import { toPlayersPage } from "./players.mapper";
import type { PlayersPage } from "./types";

async function getPage(
	page: number,
	size: number,
	signal: AbortSignal,
): Promise<PlayersPage> {
	const { data } = await http.get<unknown>("/players", {
		authenticated: true,
		params: { page, size },
		signal,
	});

	return toPlayersPage(data, page, size);
}

export const playersService = { getPage };
