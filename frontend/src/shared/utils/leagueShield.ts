/**
 * Resuelve escudos de competiciones desde `src/shared/assets/league-shields`.
 *
 * El ID identifica establemente la relación cuando está disponible; el nombre
 * actual resuelve el asset local mediante alias explícitos.
 */
const modules = import.meta.glob<{ default: string }>(
	"../assets/league-shields/*.png",
	{ eager: true },
);

const leagueSlugs: Record<string, string> = {
	bundesliga: "bundesliga",
	"premier league": "premier-league",
	"primera division": "la-liga",
	"la liga": "la-liga",
	"serie a": "serie-a",
	"ligue 1": "ligue-1",
};

const shields = new Map<string, string>();
for (const [path, module] of Object.entries(modules)) {
	const match = path.match(/\/([^/]+)\.png$/);
	if (!match) continue;
	shields.set(match[1], module.default);
}

const shieldByIdentity = new Map<string, string | null>();

function normalizeLeagueName(name: string) {
	return name.trim().toLowerCase();
}

export function getLeagueSlug(leagueName: string): string | null {
	return leagueSlugs[normalizeLeagueName(leagueName)] ?? null;
}

export function getLeagueShield(
	leagueName: string,
	leagueId?: number,
): string | null {
	const identity =
		leagueId === undefined
			? null
			: `${leagueId}:${normalizeLeagueName(leagueName)}`;
	if (identity && shieldByIdentity.has(identity)) {
		return shieldByIdentity.get(identity) ?? null;
	}
	const slug = getLeagueSlug(leagueName);
	const shield = slug ? (shields.get(slug) ?? null) : null;
	if (identity) shieldByIdentity.set(identity, shield);
	return shield;
}
