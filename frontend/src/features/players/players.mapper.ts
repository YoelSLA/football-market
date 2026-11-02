import { getLeagueClassification } from "@/features/players/constants";
import { getTeamShield } from "@/shared/utils/teamShield";
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

export function toTacticalPosition(position: string): TacticalPosition {
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
	return map[position] ?? "MED";
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

export function formatRankingDateTime(
	dateString: string | null | undefined,
): string {
	if (!dateString) return "—";
	try {
		const date = new Date(dateString);
		if (Number.isNaN(date.getTime())) return dateString;
		const formattedDate = new Intl.DateTimeFormat("es-AR", {
			day: "2-digit",
			month: "short",
			year: "numeric",
		}).format(date);
		if (dateString.includes("T")) {
			const hours = String(date.getHours()).padStart(2, "0");
			const minutes = String(date.getMinutes()).padStart(2, "0");
			return `${formattedDate} ${hours}:${minutes}`;
		}
		return formattedDate;
	} catch {
		return dateString;
	}
}

export function toScoreRankingItems(
	dto: ScoreRankingResponseDTO,
	playersMap: Map<number, Player>,
): PlayerRankingItem[] {
	return dto.ranking.map((entry) => {
		const player = playersMap.get(entry.playerId);
		const league = player ? getLeagueClassification(player.leagueName) : null;
		return {
			position: entry.position,
			playerId: entry.playerId,
			playerName: player?.name ?? `Jugador #${entry.playerId}`,
			playerAvatar: player?.imageUrl ?? player?.fallbackImageUrl ?? null,
			teamName: player?.teamName ?? "Sin club",
			teamBadge:
				player?.teamName && player?.leagueName
					? getTeamShield(player.leagueName, player.teamName)
					: null,
			leagueName: player?.leagueName ?? "Sin liga",
			leagueFlag: league?.image ?? null,
			tacticalPosition: toTacticalPosition(player?.position ?? ""),
			score: entry.score,
			formattedDate: formatRankingDate(entry.valuationDate),
		};
	});
}

export function toQuoteRankingItems(
	dto: QuoteRankingResponseDTO,
	playersMap: Map<number, Player>,
): PlayerRankingItem[] {
	return dto.ranking.map((entry) => {
		const player = playersMap.get(entry.playerId);
		const league = player ? getLeagueClassification(player.leagueName) : null;
		return {
			position: entry.position,
			playerId: entry.playerId,
			playerName: player?.name ?? `Jugador #${entry.playerId}`,
			playerAvatar: player?.imageUrl ?? player?.fallbackImageUrl ?? null,
			teamName: player?.teamName ?? "Sin club",
			teamBadge:
				player?.teamName && player?.leagueName
					? getTeamShield(player.leagueName, player.teamName)
					: null,
			leagueName: player?.leagueName ?? "Sin liga",
			leagueFlag: league?.image ?? null,
			tacticalPosition: toTacticalPosition(player?.position ?? ""),
			price: entry.price,
			formattedDate: formatRankingDateTime(
				entry.calculatedAt || entry.effectiveDate,
			),
			origin: entry.origin,
		};
	});
}

export function toScoreRankingMetadata(
	dto: ScoreRankingResponseDTO | null | undefined,
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
		totalClassified: dto?.ranking?.length ?? 0,
	};
}

export function toQuoteRankingMetadata(
	dto: QuoteRankingResponseDTO | null | undefined,
): RankingMetadata {
	const latestEntry = dto?.ranking?.[0];
	const latestDate = latestEntry?.calculatedAt || latestEntry?.effectiveDate;
	return {
		mode: "VALUATION",
		strategyName: null,
		strategyLabel: "Cotización de Mercado",
		referenceDate: formatRankingDateTime(latestDate),
		totalClassified: dto?.ranking?.length ?? 0,
		quoteOriginSummary: "Mercado",
	};
}
