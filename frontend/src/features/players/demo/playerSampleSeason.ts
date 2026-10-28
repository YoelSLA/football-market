import { DEMO_SEASON } from "@/features/players/constants";
import type { PlayerSampleSeason } from "@/features/players/types";
import { demoHash } from "@/features/players/utils";

const MATCHES_RANGE = 34;
const GOALS_RANGE = 18;
const ASSISTS_RANGE = 12;
const MINUTES_PER_MATCH = 90;

/**
 * Estadísticas de demostración derivadas de forma determinista de (playerId, season).
 *
 * Es la única fuente de valores deportivos simulados: la comparten el catálogo y el
 * detalle, de modo que ambos coincidan para el mismo jugador y temporada.
 */
export function getPlayerSampleSeason(
	playerId: number,
	season: string = DEMO_SEASON,
): PlayerSampleSeason {
	const seed = demoHash(playerId, 1);
	const matches = seed % MATCHES_RANGE;

	if (matches === 0) {
		return {
			playerId,
			season,
			matches: 0,
			goals: 0,
			assists: 0,
			minutesPlayed: 0,
			isDemo: true,
		};
	}

	const goals = demoHash(playerId, 2) % (GOALS_RANGE + 1);
	const assists = demoHash(playerId, 3) % (ASSISTS_RANGE + 1);
	const minutesPlayed = matches * MINUTES_PER_MATCH;

	return {
		playerId,
		season,
		matches,
		goals,
		assists,
		minutesPlayed,
		isDemo: true,
	};
}
