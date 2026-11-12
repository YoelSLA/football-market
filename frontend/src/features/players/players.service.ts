import axios from "axios";
import { getErrorCode, http } from "@/infrastructure/http";
import {
	toPlayerCatalogFilters,
	toPlayerDetail,
	toPlayerQuote,
	toPlayerQuoteHistory,
	toPlayersPage,
	toStatistics,
} from "./players.mapper";
import type {
	PlayerDetail,
	PlayerQuote,
	PlayerQuoteHistory,
	PlayerQuoteHistoryResponseDTO,
	PlayerQuoteResponseDTO,
	PlayerResponseDTO,
	PlayerStatistics,
	PlayersPage,
	PlayersPageResponseDTO,
	QuoteRankingResponseDTO,
	ScoreRankingResponseDTO,
} from "./types";

async function getPage(
	page: number,
	size: number,
	signal: AbortSignal,
	criteria?: import("./types").PlayerCatalogCriteria,
): Promise<PlayersPage> {
	const { data } = await http.get<PlayersPageResponseDTO>("/players", {
		authenticated: true,
		params: {
			page,
			size,
			...(criteria
				? {
						name: criteria.name || undefined,
						leagueId: criteria.leagueId || undefined,
						teamId: criteria.teamId || undefined,
						position: criteria.position || undefined,
						nationality: criteria.nationality || undefined,
						sort: criteria.sort,
						includeStatistics: true,
					}
				: {}),
		},
		signal,
	});

	return toPlayersPage(data);
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
	const { data } = await http.get<
		import("./types").PlayerStatisticsResponseDTO
	>(`/players/${id}/statistics`, {
		authenticated: true,
		params: { seasonKey: seasonKey ?? undefined },
		signal,
	});

	return toStatistics(data);
}

async function getFilters(
	signal: AbortSignal,
): Promise<import("./types").PlayerCatalogFilters> {
	const { data } = await http.get<import("./types").PlayerCatalogFiltersDTO>(
		"/players/filters",
		{ authenticated: true, signal },
	);
	return toPlayerCatalogFilters(data);
}

async function getScoreRanking(
	signal: AbortSignal,
): Promise<ScoreRankingResponseDTO> {
	const { data } = await http.get<ScoreRankingResponseDTO>(
		"/players/ranking/score",
		{ authenticated: true, signal },
	);
	return data;
}

async function getQuoteRanking(
	signal: AbortSignal,
): Promise<QuoteRankingResponseDTO> {
	const { data } = await http.get<QuoteRankingResponseDTO>("/players/ranking", {
		authenticated: true,
		signal,
	});
	return data;
}

async function getCurrentQuote(
	playerId: number,
	signal?: AbortSignal,
): Promise<PlayerQuote | null> {
	try {
		const { data } = await http.get<PlayerQuoteResponseDTO>(
			`/players/${playerId}/quotes/current`,
			{
				authenticated: true,
				signal,
			},
		);
		return toPlayerQuote(data);
	} catch (error: unknown) {
		if (axios.isCancel(error)) {
			throw error;
		}
		if (
			axios.isAxiosError(error) &&
			error.response?.status === 404 &&
			getErrorCode(error) === "QUOTE_NOT_FOUND"
		) {
			return null;
		}
		throw error;
	}
}

async function getQuoteHistory(
	playerId: number,
	signal?: AbortSignal,
): Promise<PlayerQuoteHistory> {
	const { data } = await http.get<PlayerQuoteHistoryResponseDTO>(
		`/players/${playerId}/quotes`,
		{
			authenticated: true,
			signal,
		},
	);
	return toPlayerQuoteHistory(data);
}

export const playersService = {
	getPage,
	getById,
	getStatistics,
	getFilters,
	getScoreRanking,
	getQuoteRanking,
	getCurrentQuote,
	getQuoteHistory,
};
