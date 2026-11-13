import { getLeagueClassification } from "@/features/players/constants";
import type {
	Player,
	PlayerCatalogFiltersDTO,
	PlayerDetail,
	PlayerMetric,
	PlayerMetricResponseDTO,
	PlayerRankingItem,
	PlayerResponseDTO,
	PlayerStatistics,
	PlayerStatisticsResponseDTO,
	PlayerStatisticsState,
	PlayersPage,
	PlayersPageResponseDTO,
	QuoteRankingResponseDTO,
	RankingMetadata,
	ScoreRankingResponseDTO,
	TacticalPosition,
} from "./types";

export function toPlayerDetail(dto: PlayerResponseDTO): PlayerDetail {
	return {
		id: dto.id,
		name: dto.name,
		teamId: dto.teamId,
		teamName: dto.teamName,
		leagueId: dto.leagueId,
		leagueName: dto.leagueName,
		position: dto.position,
		dateOfBirth: dto.dateOfBirth,
		nationality: dto.nationality,
		imageUrl: dto.imageUrl,
		fallbackImageUrl: dto.fallbackImageUrl,
		active: true,
	};
}

export function toPlayersPage(dto: PlayersPageResponseDTO): PlayersPage {
	return {
		players: dto.content.map(toPlayer),
		page: dto.page,
		size: dto.size,
		totalElements: dto.totalElements,
		totalPages: dto.totalPages,
	};
}

export function toPlayer(dto: PlayerResponseDTO): Player {
	return {
		id: dto.id,
		name: dto.name,
		teamId: dto.teamId,
		teamName: dto.teamName,
		leagueId: dto.leagueId,
		leagueName: dto.leagueName,
		position: dto.position,
		dateOfBirth: dto.dateOfBirth,
		nationality: dto.nationality,
		imageUrl: dto.imageUrl,
		fallbackImageUrl: dto.fallbackImageUrl,
		active: true,
		statistics: dto.statistics
			? { ...dto.statistics, sources: { ...dto.statistics.sources } }
			: undefined,
	};
}

export function toPlayerCatalogFilters(
	dto: PlayerCatalogFiltersDTO,
): import("./types").PlayerCatalogFilters {
	return {
		leagues: dto.leagues.map((item) => ({ ...item })),
		teams: dto.teams.map((item) => ({ ...item })),
		positions: [...dto.positions],
		nationalities: [...dto.nationalities],
	};
}

export function toMetric(dto: PlayerMetricResponseDTO): PlayerMetric {
	return {
		key: dto.key,
		unit: dto.unit,
		value: dto.value,
		source: dto.source,
	};
}

export function toStatistics(
	dto: PlayerStatisticsResponseDTO,
): PlayerStatistics {
	return {
		playerId: dto.playerId,
		requestedSeasonKey: dto.requestedSeasonKey,
		status: dto.status,
		seasonKey: dto.seasonKey,
		seasonLabel: dto.seasonLabel,
		extractionDate: dto.extractionDate,
		competitionKey: dto.competitionKey,
		competitionName: dto.competitionName,
		metrics: dto.metrics.map(toMetric),
	};
}

export function toStatisticsState(
	dto: PlayerStatisticsResponseDTO | null,
	errorMessage: string | null,
): PlayerStatisticsState {
	if (!dto) {
		return errorMessage
			? { status: "error", data: null, errorMessage }
			: { status: "idle", data: null, errorMessage: null };
	}

	const stats = toStatistics(dto);

	return {
		status: "ready",
		data: stats,
		errorMessage,
	};
}

export function toTacticalPosition(
	position: string | null | undefined,
): TacticalPosition | null {
	if (!position) return null;

	const map: Record<string, TacticalPosition> = {
		Goalkeeper: "POR",
		Defence: "DEF",
		Defender: "DEF",
		Midfield: "MED",
		Midfielder: "MED",
		Offence: "DEL",
		Forward: "DEL",
		POR: "POR",
		DEF: "DEF",
		MED: "MED",
		DEL: "DEL",
	};
	return map[position] ?? null;
}

export function formatRankingDate(
	dateString: string | null | undefined,
): string {
	if (!dateString) return "—";
	try {
		const parts = dateString.split("T")[0].split("-");
		if (parts.length === 3) {
			const year = Number.parseInt(parts[0], 10);
			const month = Number.parseInt(parts[1], 10) - 1;
			const day = Number.parseInt(parts[2], 10);
			const date = new Date(year, month, day);
			return new Intl.DateTimeFormat("es-AR", {
				day: "2-digit",
				month: "short",
				year: "numeric",
			}).format(date);
		}
		const date = new Date(dateString);
		if (Number.isNaN(date.getTime())) return dateString;
		return new Intl.DateTimeFormat("es-AR", {
			day: "2-digit",
			month: "short",
			year: "numeric",
		}).format(date);
	} catch {
		return dateString;
	}
}

function toRankingIdentity(entry: {
	position: number;
	playerId: number;
	player: import("./types").RankingPlayerDTO;
}): Omit<PlayerRankingItem, "score" | "price" | "formattedDate" | "origin"> {
	const league = entry.player.league;

	return {
		position: entry.position,
		playerId: entry.playerId,
		playerName: entry.player.name,
		playerAvatar: entry.player.imageUrl,
		teamName: entry.player.team?.name ?? null,
		teamBadge: entry.player.team?.crestUrl ?? null,
		leagueName: league?.name ?? null,
		leagueFlag: league ? getLeagueClassification(league.name).image : null,
		tacticalPosition: toTacticalPosition(entry.player.position),
	};
}

export function toScoreRankingItems(
	dto: ScoreRankingResponseDTO,
): PlayerRankingItem[] {
	return dto.content.map((entry) => ({
		...toRankingIdentity(entry),
		score: entry.score,
		formattedDate: formatRankingDate(entry.valuationDate),
	}));
}

export function toQuoteRankingItems(
	dto: QuoteRankingResponseDTO,
): PlayerRankingItem[] {
	return dto.content.map((entry) => ({
		...toRankingIdentity(entry),
		price: entry.price / 100,
		formattedDate: formatRankingDate(entry.effectiveDate),
		origin: entry.origin,
	}));
}

export function toRankingFilterOptions(
	dto: import("./types").RankingFilterOptionsDTO,
): import("./types").RankingFilterOptions {
	return {
		leagues: dto.leagues.map((league) => ({ ...league })),
		positions: [...dto.positions],
	};
}

export function toScoreRankingPage(
	dto: ScoreRankingResponseDTO,
): import("./types").RankingPage {
	return {
		items: toScoreRankingItems(dto),
		page: dto.page,
		size: dto.size,
		totalElements: dto.totalElements,
		totalPages: dto.totalPages,
		filterOptions: toRankingFilterOptions(dto.filterOptions),
	};
}

export function toQuoteRankingPage(
	dto: QuoteRankingResponseDTO,
): import("./types").RankingPage {
	return {
		items: toQuoteRankingItems(dto),
		page: dto.page,
		size: dto.size,
		totalElements: dto.totalElements,
		totalPages: dto.totalPages,
		filterOptions: toRankingFilterOptions(dto.filterOptions),
	};
}

export function toScoreRankingMetadata(
	dto: ScoreRankingResponseDTO | null | undefined,
	globalTotal: number | null,
): RankingMetadata {
	const strategyLabels: Record<string, string> = {
		POSITIONAL_PERFORMANCE: "Desempeño por posición",
		GENERAL_ATTRIBUTES: "Atributos generales",
	};
	const strategyName = dto?.strategy ?? null;
	const strategyLabel = strategyName
		? (strategyLabels[strategyName] ?? strategyName)
		: "Desempeño por posición";

	return {
		mode: "PERFORMANCE",
		strategyName,
		strategyLabel,
		referenceDate: formatRankingDate(dto?.quoteDate),
		totalClassified: globalTotal,
	};
}

export function toQuoteRankingMetadata(
	dto: QuoteRankingResponseDTO | null | undefined,
	globalTotal: number | null,
): RankingMetadata {
	return {
		mode: "VALUATION",
		strategyName: null,
		strategyLabel: "Cotización de mercado",
		referenceDate: formatRankingDate(dto?.content?.[0]?.effectiveDate),
		totalClassified: globalTotal,
	};
}
