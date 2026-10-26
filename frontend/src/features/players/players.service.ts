import { http } from "@/infrastructure/http";
import { toPlayerDetail, toPlayersPage, toStatistics, toPlayerCatalogFilters } from "./players.mapper";
import type { PlayerDetail, PlayersPage, PlayerStatistics } from "./types";
import type { PlayerResponseDTO, PlayersPageResponseDTO } from "./types";

async function getPage(
	page: number,
	size: number,
	signal: AbortSignal,
	criteria?: import("./types").PlayerCatalogCriteria,
): Promise<PlayersPage> {
	const { data } = await http.get<PlayersPageResponseDTO>("/players", {
		authenticated: true,
		params: { page, size, ...(criteria ? { name: criteria.name || undefined, leagueId: criteria.leagueId || undefined, teamId: criteria.teamId || undefined, position: criteria.position || undefined, nationality: criteria.nationality || undefined, sort: criteria.sort, includeStatistics: true } : {}) },
		signal,
	});

	return toPlayersPage(data, page, size);
}

async function getById(id: number, signal: AbortSignal): Promise<PlayerDetail> {
	const { data } = await http.get<PlayerResponseDTO>(`/players/${id}`, {
		authenticated: true,
		signal,
	});

	return toPlayerDetail(data);
}

async function getStatistics(
	id: number,
	seasonKey: string | null,
	signal: AbortSignal,
): Promise<PlayerStatistics> {
	const { data } = await http.get<import("./types").PlayerStatisticsResponseDTO>(`/players/${id}/statistics`, {
		authenticated: true,
		params: { seasonKey: seasonKey ?? undefined },
		signal,
	});

	return toStatistics(data);
}

async function getFilters(signal: AbortSignal): Promise<import("./types").PlayerCatalogFilters> {
	const { data } = await http.get<import("./types").PlayerCatalogFiltersDTO>("/players/filters", { authenticated: true, signal });
	return toPlayerCatalogFilters(data);
}

export const playersService = { getPage, getById, getStatistics, getFilters };
