/**
 * Resuelve escudos de competiciones desde `src/shared/assets/league-shields`.
 *
 * La asociación se realiza por el nombre de la liga (con alias explícitos para
 * diferencias de nombre), nunca por posición en un listado.
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

function normalizeLeagueName(name: string) {
	return name.trim().toLowerCase();
}

export function getLeagueSlug(leagueName: string): string | null {
	return leagueSlugs[normalizeLeagueName(leagueName)] ?? null;
}

export function getLeagueShield(leagueName: string): string | null {
	const slug = getLeagueSlug(leagueName);
	if (!slug) return null;
	return shields.get(slug) ?? null;
}