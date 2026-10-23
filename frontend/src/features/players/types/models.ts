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
