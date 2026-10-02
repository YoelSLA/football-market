export interface PlayerResponseDTO {
	id: number;
	name: string;
	team: string;
	league: string;
	position: string;
	dateOfBirth: string | null;
	nationality: string | null;
	imageUrl: string | null;
	fallbackImageUrl: string | null;
}

export interface PlayersPageResponseDTO {
	content: PlayerResponseDTO[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
}
