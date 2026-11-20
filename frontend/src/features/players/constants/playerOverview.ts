export const HEADLINE_METRIC_KEYS = [
	[
		"saves_per_match",
		"pass_completion_percentage",
		"successful_long_balls_per_match",
	],
	["tackles_per_match", "interceptions_per_match", "clearances_per_match"],
	["passes_per_match", "pass_completion_percentage", "key_passes_per_match"],
	["goals_total", "shots_per_match", "assists_total"],
] as const;

export const HEADLINE_METRIC_LABELS: Record<string, string> = {
	saves_per_match: "Atajadas",
	pass_completion_percentage: "Precisión de pases",
	successful_long_balls_per_match: "Pases largos acertados",
	tackles_per_match: "Entradas",
	interceptions_per_match: "Intercepciones",
	clearances_per_match: "Despejes",
	passes_per_match: "Pases por partido",
	key_passes_per_match: "Pases clave",
	goals_total: "Goles",
	shots_per_match: "Tiros por partido",
	assists_total: "Asistencias",
};

export function getHeadlineMetricLabel(key: string): string {
	return HEADLINE_METRIC_LABELS[key] ?? key;
}

export function getHeadlineMetricUnit(key: string): string {
	if (key === "pass_completion_percentage") return "% acierto";
	if (key.endsWith("_total")) return "total temporada";
	return "por partido";
}
