export interface PlayerResponseDTO {
	id: number;
	name: string;
	team: string;
	league: string;
	position: string;
}

export interface PlayersPageResponseDTO {
	content: PlayerResponseDTO[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
}
