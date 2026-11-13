export interface PlayerResponseDTO {
	id: number;
	name: string;
	teamId: number;
	teamName: string;
	leagueId: number;
	leagueName: string;
	position: string;
	dateOfBirth: string | null;
	nationality: string | null;
	imageUrl: string | null;
	fallbackImageUrl: string | null;
	statistics?: PlayerCatalogStatisticsDTO;
}

export interface PlayerCatalogStatisticsDTO {
	seasonKey: string | null;
	seasonLabel: string | null;
	extractionDate: string | null;
	competitionKey: string | null;
	appearancesTotal: number | null;
	goalsTotal: number | null;
	assistsTotal: number | null;
	sources: {
		appearancesTotal: "JSON" | "SCRAPING" | null;
		goalsTotal: "JSON" | "SCRAPING" | null;
		assistsTotal: "JSON" | "SCRAPING" | null;
	};
}

export interface PlayerCatalogFiltersDTO {
	leagues: { id: number; name: string }[];
	teams: { id: number; name: string; leagueId: number }[];
	positions: string[];
	nationalities: string[];
}

export interface PlayersPageResponseDTO {
	content: PlayerResponseDTO[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
}

export interface PlayerMetricResponseDTO {
	key: string;
	unit: "SEASON_TOTAL" | "PER_MATCH" | "PERCENTAGE" | "AVERAGE_RATING";
	value: number | null;
	source: "JSON" | "SCRAPING" | null;
}

export interface PlayerStatisticsResponseDTO {
	playerId: number;
	requestedSeasonKey: string | null;
	status: "AVAILABLE" | "PARTIAL" | "NO_DATA";
	seasonKey: string | null;
	seasonLabel: string | null;
	extractionDate: string | null;
	competitionKey: string | null;
	competitionName: string | null;
	metrics: PlayerMetricResponseDTO[];
}

export interface RankingPlayerDTO {
	name: string;
	position: string | null;
	imageUrl: string | null;
	team: {
		id: number;
		name: string;
		crestUrl: string | null;
	} | null;
	league: {
		id: number;
		name: string;
	} | null;
}

export interface RankingFilterOptionsDTO {
	leagues: { id: number; name: string }[];
	positions: string[];
}

export interface ScoreRankingEntryDTO {
	position: number;
	playerId: number;
	score: number;
	valuationDate: string;
	player: RankingPlayerDTO;
}

export interface ScoreRankingResponseDTO {
	strategy: string | null;
	quoteDate: string | null;
	content: ScoreRankingEntryDTO[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
	filterOptions: RankingFilterOptionsDTO;
}

export interface QuoteRankingEntryDTO {
	position: number;
	playerId: number;
	/** Precio vigente en centésimas. */
	price: number;
	effectiveDate: string;
	origin: "INITIAL" | "RECOMPUTED" | "CARRIED_FORWARD";
	calculatedAt: string;
	player: RankingPlayerDTO;
}

export interface QuoteRankingResponseDTO {
	content: QuoteRankingEntryDTO[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
	filterOptions: RankingFilterOptionsDTO;
}
