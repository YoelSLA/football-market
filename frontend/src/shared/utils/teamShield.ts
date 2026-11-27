/**
 * Resuelve escudos de equipos desde `src/shared/assets/team-shields/{liga}/{equipo}.png`.
 *
 * Los IDs identifican establemente la relación cuando están disponibles; liga y
 * nombre actuales resuelven el asset local. Las diferencias de nombre,
 * abreviaturas y caracteres especiales se cubren con alias explícitos.
 */
import { getLeagueSlug } from "./leagueShield";

const modules = import.meta.glob<{ default: string }>(
	"../assets/team-shields/*/*.png",
	{ eager: true },
);

// leagueSlug -> (teamSlug -> url)
const shields = new Map<string, Map<string, string>>();
for (const [path, module] of Object.entries(modules)) {
	const match = path.match(/\/team-shields\/([^/]+)\/([^/]+)\.png$/);
	if (!match) continue;
	const leagueSlug = match[1];
	const teamSlug = match[2];
	if (!shields.has(leagueSlug)) shields.set(leagueSlug, new Map());
	shields.get(leagueSlug)?.set(teamSlug, module.default);
}

const shieldByIdentity = new Map<string, string | null>();

/** Normaliza un nombre a la forma de slug usada por los archivos. */
export function normalizeTeamSlug(name: string): string {
	return name
		.normalize("NFC")
		.trim()
		.toLowerCase()
		.replace(/\s+/g, "-")
		.replace(/-+/g, "-");
}

/**
 * Alias explícitos de slug de equipo por liga. La clave es el slug normalizado
 * del nombre que devuelve el backend; el valor es el slug del archivo local.
 */
const teamAliases: Record<string, Record<string, string>> = {
	"serie-a": {
		"udinese-calcio": "unidese-calcio",
	},
};

export function getTeamShield(
	leagueName: string,
	teamName: string,
	teamId?: number,
	leagueId?: number,
): string | null {
	const identity =
		teamId === undefined
			? null
			: `${leagueId ?? "unknown"}:${teamId}:${leagueName}:${teamName}`;
	if (identity && shieldByIdentity.has(identity)) {
		return shieldByIdentity.get(identity) ?? null;
	}
	const leagueSlug = getLeagueSlug(leagueName);
	if (!leagueSlug) {
		if (identity) shieldByIdentity.set(identity, null);
		return null;
	}
	const leagueShields = shields.get(leagueSlug);
	if (!leagueShields) {
		if (identity) shieldByIdentity.set(identity, null);
		return null;
	}
	const slug = normalizeTeamSlug(teamName);
	const canonical = teamAliases[leagueSlug]?.[slug] ?? slug;
	const shield = leagueShields.get(canonical) ?? null;
	if (identity) shieldByIdentity.set(identity, shield);
	return shield;
}
