import type { PlayerSampleStatistics } from "../types";

export const PLAYER_SAMPLE_STATISTICS: PlayerSampleStatistics = {
	matches: 24,
	goals: 8,
	assists: 5,
};

export const PLAYER_SAMPLE_STATISTIC_ITEMS = [
	{ key: "matches", label: "Partidos" },
	{ key: "goals", label: "Goles" },
	{ key: "assists", label: "Asistencias" },
] as const;
