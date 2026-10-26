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
