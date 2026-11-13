export interface Player {
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
	active: true;
	statistics?: PlayerCatalogStatistics;
}

export interface PlayerCatalogStatistics {
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

export interface PlayerCatalogFilters {
	leagues: { id: number; name: string }[];
	teams: { id: number; name: string; leagueId: number }[];
	positions: string[];
	nationalities: string[];
}

export interface PlayerCatalogCriteria {
	name: string;
	leagueId: string;
	teamId: string;
	position: string;
	nationality: string;
	sort: "name,asc" | "name,desc";
	page: number;
}

/**
 * Identidad real consultada de forma individual. El endpoint de detalle solo devuelve
 * jugadores visibles para el catálogo, por lo que `active` es siempre `true`.
 */
export interface PlayerDetail {
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
	active: true;
}

export interface PlayersPage {
	players: Player[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
}

export interface PlayerSampleStatistics {
	matches: number;
	goals: number;
	assists: number;
}

/** Resumen deportivo de demostración para un jugador en la temporada fija. */
export interface PlayerSampleSeason {
	playerId: number;
	season: string;
	matches: number;
	goals: number;
	assists: number;
	minutesPlayed: number;
	isDemo: true;
}

/** Cotización diaria de demostración del token de un jugador. */
export interface TokenQuotePoint {
	playerId: number;
	date: string;
	price: number;
	isDemo: true;
}

/** Cotización actual y variación derivadas de la serie. */
export interface TokenQuoteSnapshot {
	playerId: number;
	price: number | null;
	date: string | null;
	absoluteChange: number | null;
	percentageChange: number | null;
	direction: "up" | "down" | "flat" | "unknown";
	isDemo: true;
}

/** Serie histórica de demostración con su snapshot derivado. */
export interface TokenQuoteSeries {
	playerId: number;
	points: TokenQuotePoint[];
	snapshot: TokenQuoteSnapshot;
}

/** Valor original persistido: ausencia y cero son estados diferentes. */
export interface PlayerMetric {
	key: string;
	unit: "SEASON_TOTAL" | "PER_MATCH" | "PERCENTAGE" | "AVERAGE_RATING";
	value: number | null;
	source: "JSON" | "SCRAPING" | null;
}

export interface PlayerStatistics {
	playerId: number;
	requestedSeasonKey: string | null;
	status: "AVAILABLE" | "PARTIAL" | "NO_DATA";
	seasonKey: string | null;
	seasonLabel: string | null;
	extractionDate: string | null;
	competitionKey: string | null;
	competitionName: string | null;
	metrics: PlayerMetric[];
}

/** Estado de consulta; un error de actualización no altera la respuesta anterior. */
export type PlayerStatisticsState =
	| { status: "idle" | "loading"; data: null; errorMessage: null }
	| { status: "error"; data: null; errorMessage: string }
	| { status: "ready"; data: PlayerStatistics; errorMessage: string | null };

export type TacticalPosition = "DEL" | "MED" | "DEF" | "POR";

export type RankingMode = "PERFORMANCE" | "VALUATION";

export type RankingOrigin = "INITIAL" | "RECOMPUTED" | "CARRIED_FORWARD";

/** Parámetros efectivos enviados al endpoint de ranking. `page` es base cero. */
export interface RankingQuery {
	page: number;
	size: number;
	name?: string;
	leagueId?: number;
	position?: string;
}

export interface RankingFilterOptions {
	leagues: { id: number; name: string }[];
	positions: string[];
}

export interface PlayerRankingItem {
	position: number;
	playerId: number;
	playerName: string;
	playerAvatar: string | null;
	teamName: string | null;
	teamBadge: string | null;
	leagueName: string | null;
	leagueFlag: string | null;
	tacticalPosition: TacticalPosition | null;
	score?: number;
	/** Cotización en créditos, convertida desde las centésimas del contrato. */
	price?: number;
	formattedDate: string;
	origin?: RankingOrigin;
}

export interface RankingPage {
	items: PlayerRankingItem[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
	filterOptions: RankingFilterOptions;
}

export interface RankingMetadata {
	mode: RankingMode;
	strategyName: string | null;
	strategyLabel: string;
	referenceDate: string | null;
	totalClassified: number | null;
}

export interface RankingFilterCriteria {
	name: string;
	leagueId: string;
	position: string;
}
