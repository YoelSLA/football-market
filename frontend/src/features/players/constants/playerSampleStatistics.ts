import { getPlayerSampleSeason } from "@/features/players/demo";

export const PLAYER_SAMPLE_STATISTIC_ITEMS = [
	{ key: "matches", label: "Partidos" },
	{ key: "goals", label: "Goles" },
	{ key: "assists", label: "Asistencias" },
] as const;

export type PlayerSampleStatisticKey =
	(typeof PLAYER_SAMPLE_STATISTIC_ITEMS)[number]["key"];

/**
 * Estadísticas de muestra compartidas entre catálogo y detalle para un jugador.
 * Son datos de demostración, no rendimiento real de la temporada.
 */
export function getPlayerSampleStatistics(playerId: number) {
	const season = getPlayerSampleSeason(playerId);

	return {
		matches: season.matches,
		goals: season.goals,
		assists: season.assists,
	};
}
