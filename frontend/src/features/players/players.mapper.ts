import type {
	Player,
	PlayerResponseDTO,
	PlayersPage,
	PlayersPageResponseDTO,
} from "./types";

function isRecord(value: unknown): value is Record<string, unknown> {
	return typeof value === "object" && value !== null;
}

function isNonNegativeInteger(value: unknown): value is number {
	return typeof value === "number" && Number.isInteger(value) && value >= 0;
}

function isPlayerResponseDTO(value: unknown): value is PlayerResponseDTO {
	return (
		isRecord(value) &&
		isNonNegativeInteger(value.id) &&
		typeof value.name === "string" &&
		isNonNegativeInteger(value.teamId) &&
		typeof value.teamName === "string" &&
		isNonNegativeInteger(value.leagueId) &&
		typeof value.leagueName === "string" &&
		typeof value.position === "string" &&
		(value.dateOfBirth === null || typeof value.dateOfBirth === "string") &&
		(value.nationality === null || typeof value.nationality === "string") &&
		(value.imageUrl === null || typeof value.imageUrl === "string") &&
		(value.fallbackImageUrl === null ||
			typeof value.fallbackImageUrl === "string")
	);
}

function parsePlayersPageResponseDTO(
	value: unknown,
	expectedPage: number,
	expectedSize: number,
): PlayersPageResponseDTO {
	if (!isRecord(value) || !Array.isArray(value.content)) {
		throw new Error("La respuesta del catálogo de jugadores no es válida.");
	}

	const { content, page, size, totalElements, totalPages } = value;
	const hasValidMetadata =
		isNonNegativeInteger(page) &&
		isNonNegativeInteger(size) &&
		size >= 1 &&
		size <= 100 &&
		isNonNegativeInteger(totalElements) &&
		isNonNegativeInteger(totalPages);
	const hasCoherentPagination =
		hasValidMetadata &&
		page === expectedPage &&
		size === expectedSize &&
		totalPages === Math.ceil(totalElements / size) &&
		content.length <= size &&
		content.length <= 12 &&
		content.length <= totalElements &&
		(content.length === 0 || page < totalPages);

	if (!hasCoherentPagination || !content.every(isPlayerResponseDTO)) {
		throw new Error("La respuesta del catálogo de jugadores no es válida.");
	}

	return { content, page, size, totalElements, totalPages };
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
	};
}

export function toPlayersPage(
	value: unknown,
	expectedPage: number,
	expectedSize: number,
): PlayersPage {
	const dto = parsePlayersPageResponseDTO(value, expectedPage, expectedSize);

	return {
		players: dto.content.map(toPlayer),
		page: dto.page,
		size: dto.size,
		totalElements: dto.totalElements,
		totalPages: dto.totalPages,
	};
}
