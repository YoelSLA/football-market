import { http } from "@/infrastructure/http";
import { toPlayerDetail, toPlayersPage } from "./players.mapper";
import type { PlayerDetail, PlayersPage } from "./types";

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

async function getById(id: number, signal: AbortSignal): Promise<PlayerDetail> {
	const { data } = await http.get<unknown>(`/players/${id}`, {
		authenticated: true,
		signal,
	});

	return toPlayerDetail(data);
}

export const playersService = { getPage, getById };
