import type { PlayerCatalogCriteria } from "../types";

export const DEFAULT_CATALOG_CRITERIA: PlayerCatalogCriteria = {
	name: "",
	leagueId: "",
	teamId: "",
	position: "",
	nationality: "",
	sort: "name,asc",
	page: 0,
};

const VALID_POSITIONS = new Set([
	"Goalkeeper",
	"Defender",
	"Defence",
	"Midfield",
	"Midfielder",
	"Offence",
	"Forward",
]);

/**
 * Deserializa y valida los parámetros de búsqueda de la URL.
 * Limpia y normaliza parámetros inválidos a sus valores seguros predeterminados.
 */
export function parseCatalogSearchParams(
	searchParams: URLSearchParams,
): PlayerCatalogCriteria {
	const rawName = searchParams.get("name") ?? "";
	const rawLeagueId = searchParams.get("leagueId") ?? "";
	const rawTeamId = searchParams.get("teamId") ?? "";
	const rawPosition = searchParams.get("position") ?? "";
	const rawNationality = searchParams.get("nationality") ?? "";
	const rawSort = searchParams.get("sort");
	const rawPage = searchParams.get("page");

	// Validación de página: entero >= 0
	const parsedPage = rawPage ? parseInt(rawPage, 10) : 0;
	const validPage =
		Number.isInteger(parsedPage) && parsedPage >= 0 ? parsedPage : 0;

	// Validación de sort: únicamente "name,asc" o "name,desc"
	const validSort =
		rawSort === "name,desc" ? ("name,desc" as const) : ("name,asc" as const);

	// Validación de IDs numéricos
	const validLeagueId =
		rawLeagueId && /^\d+$/.test(rawLeagueId) ? rawLeagueId : "";
	const validTeamId = rawTeamId && /^\d+$/.test(rawTeamId) ? rawTeamId : "";

	// Validación de posición (si no está en el conjunto permitido, ignorar)
	const validPosition =
		rawPosition && VALID_POSITIONS.has(rawPosition) ? rawPosition : "";

	return {
		name: rawName.trim(),
		leagueId: validLeagueId,
		teamId: validTeamId,
		position: validPosition,
		nationality: rawNationality.trim(),
		sort: validSort,
		page: validPage,
	};
}

/**
 * Serializa los criterios del catálogo a URLSearchParams,
 * omitiendo valores predeterminados o vacíos para mantener URLs canónicas y limpias.
 */
export function serializeCatalogCriteria(
	criteria: PlayerCatalogCriteria,
): URLSearchParams {
	const params = new URLSearchParams();

	const trimmedName = criteria.name.trim();
	if (trimmedName) {
		params.set("name", trimmedName);
	}
	if (criteria.leagueId) {
		params.set("leagueId", criteria.leagueId);
	}
	if (criteria.teamId) {
		params.set("teamId", criteria.teamId);
	}
	if (criteria.position && VALID_POSITIONS.has(criteria.position)) {
		params.set("position", criteria.position);
	}
	if (criteria.nationality.trim()) {
		params.set("nationality", criteria.nationality.trim());
	}
	if (criteria.sort && criteria.sort !== "name,asc") {
		params.set("sort", criteria.sort);
	}
	if (criteria.page > 0) {
		params.set("page", String(criteria.page));
	}

	return params;
}

/**
 * Validador estricto de rutas de retorno permitidas.
 * Solo admite `/players` y `/players/ranking` (con o sin query params).
 */
export function validateReturnRoute(from: unknown): {
	isValid: boolean;
	path: string;
	label: string;
} {
	if (typeof from !== "string" || !from.startsWith("/")) {
		return { isValid: false, path: "/players", label: "Volver a jugadores" };
	}

	try {
		const url = new URL(from, "http://localhost");
		const pathname = url.pathname;
		const search = url.search;

		if (pathname === "/players/ranking") {
			return {
				isValid: true,
				path: `${pathname}${search}`,
				label: "Volver al ranking",
			};
		}

		if (pathname === "/players") {
			return {
				isValid: true,
				path: `${pathname}${search}`,
				label: "Volver a jugadores",
			};
		}

		return { isValid: false, path: "/players", label: "Volver a jugadores" };
	} catch {
		return { isValid: false, path: "/players", label: "Volver a jugadores" };
	}
}
