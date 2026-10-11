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
}

export interface PlayersPageResponseDTO {
	content: PlayerResponseDTO[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
}
