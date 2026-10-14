import { teamLogoEntries } from "../assets/teams";

function normalizeTeamName(name: string) {
	return name.normalize("NFKC").trim().replace(/\s+/g, " ").toLowerCase();
}

/**
 * Resuelve el escudo local a partir del nombre de equipo que devuelve el backend.
 *
 * Solo se indexa el nombre persistido del equipo: los alias de Wikidata son
 * conocimiento auxiliar para la importación y no autorizan coincidencias parciales
 * en tiempo de ejecución, donde un nombre genérico podría asignar un escudo ajeno.
 * Cualquier colisión entre nombres persistidos se resuelve como `null`.
 */
const logos = new Map<string, string | null>();
for (const entry of teamLogoEntries) {
	const key = normalizeTeamName(entry.name);
	logos.set(key, logos.has(key) ? null : entry.url);
}

/** Devuelve `null` si el equipo no tiene un recurso verificado o su carga falla. */
export function getTeamLogo(teamName: string): string | null {
	return logos.get(normalizeTeamName(teamName)) ?? null;
}
