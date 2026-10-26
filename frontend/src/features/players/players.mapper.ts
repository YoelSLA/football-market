import type {
	Player,
	PlayerCatalogFiltersDTO,
	PlayerDetail,
	PlayerMetric,
	PlayerMetricResponseDTO,
	PlayerResponseDTO,
	PlayerStatistics,
	PlayerStatisticsResponseDTO,
	PlayerStatisticsState,
	PlayersPage,
	PlayersPageResponseDTO,
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
