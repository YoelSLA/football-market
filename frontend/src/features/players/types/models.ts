export interface Player {
	id: number;
	name: string;
	team: string;
	league: string;
	position: string;
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
