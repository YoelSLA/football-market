import { METRIC_DEFINITIONS, getPositionProfile } from "@/features/players/constants";
import type { PlayerMetric, PlayerStatistics } from "@/features/players/types";

export function getStatisticsPosition(position: string): number {
	const names = ["goalkeeper", "defence", "midfield", "offence"];
	const aliases: Record<string, string> = { defender: "defence", midfielder: "midfield", forward: "offence" };
	const name = position.trim().toLowerCase();
	return names.indexOf(aliases[name] ?? name);
}

export function getStatisticsPositionLabel(position: string): string {
	return ["Arquero", "Defensor", "Mediocampista", "Delantero"][getStatisticsPosition(position)] ?? position;
}

export function getMetricUnit(key: string): string {
	const definition = METRIC_DEFINITIONS.find((item) => item.key === key);
	if (!definition) return "";
	if (key.endsWith("_per_match")) return `${definition.unit}/partido`;
	if (key.endsWith("_total")) return `${definition.unit} · total temporada`;
	return key === "whoscored_rating_average" ? "puntos · promedio" : definition.unit;
}

export function getStatisticsMetric(statistics: PlayerStatistics, key: string) {
	const definition = METRIC_DEFINITIONS.find((item) => item.key === key);
	const metric = statistics.metrics.find((item) => item.key === key);
	return { key, label: definition?.label ?? key, category: definition?.category ?? "General", unit: getMetricUnit(key), value: metric?.value ?? null };
}

export function formatStatisticsValue(value: number | null): string {
	return value === null ? "Sin datos" : new Intl.NumberFormat("es-AR", { maximumFractionDigits: 2 }).format(value);
}

/** Prioridades de la matriz de SPEC-009, no puntuaciones calculadas. */
const principal = [
	"minutes_played_total appearances_total starts_total whoscored_rating_average pass_completion_percentage successful_long_balls_per_match saves_per_match six_yard_box_saves_per_match penalty_area_saves_per_match outside_box_saves_per_match",
	"minutes_played_total appearances_total starts_total whoscored_rating_average pass_completion_percentage aerial_duels_won_per_match tackles_per_match interceptions_per_match clearances_per_match blocked_shots_per_match dribbled_past_per_match attempted_tackles_per_match blocked_crosses_per_match blocked_passes_per_match",
	"minutes_played_total appearances_total starts_total whoscored_rating_average assists_total assists_per_match pass_completion_percentage passes_per_match successful_long_balls_per_match accurate_short_passes_per_match key_passes_per_match long_key_passes_per_match short_key_passes_per_match successful_crosses_per_match successful_dribbles_per_match dispossessed_per_match bad_controls_per_match tackles_per_match interceptions_per_match",
	"minutes_played_total appearances_total starts_total whoscored_rating_average goals_total assists_total shots_per_match shots_on_target_total goals_per_match assists_per_match six_yard_box_shots_per_match penalty_area_shots_per_match outside_box_shots_per_match six_yard_box_goals_per_match penalty_area_goals_per_match outside_box_goals_per_match offsides_per_match key_passes_per_match successful_dribbles_per_match failed_dribbles_per_match total_dribbles_per_match dispossessed_per_match bad_controls_per_match aerial_duels_won_per_match",
].map((keys) => new Set(keys.split(" ")));
const secondaryGoalkeeper = new Set("substitute_appearances_total passes_per_match inaccurate_long_balls_per_match accurate_short_passes_per_match inaccurate_short_passes_per_match key_passes_per_match long_key_passes_per_match dispossessed_per_match bad_controls_per_match aerial_duels_won_per_match aerial_duels_lost_per_match total_aerial_duels_per_match clearances_per_match own_goals_total yellow_cards_total red_cards_total second_yellow_total fouls_committed_per_match".split(" "));
const lowRelevance = [
	new Set<string>(),
	new Set("six_yard_box_goals_per_match penalty_area_goals_per_match outside_box_goals_per_match cross_assists_per_match corner_assists_per_match through_ball_assists_per_match free_kick_assists_per_match throw_in_assists_per_match other_assists_per_match saves_per_match six_yard_box_saves_per_match penalty_area_saves_per_match outside_box_saves_per_match".split(" ")),
	new Set("throw_in_assists_per_match own_goals_total saves_per_match six_yard_box_saves_per_match penalty_area_saves_per_match outside_box_saves_per_match".split(" ")),
	new Set("offsides_won_per_match own_goals_total blocked_crosses_per_match saves_per_match six_yard_box_saves_per_match penalty_area_saves_per_match outside_box_saves_per_match".split(" ")),
];
export function getMetricPriority(key: string, position: string): number {
	const index = getStatisticsPosition(position);
	if (index < 0) return 1;
	if (principal[index].has(key)) return 0;
	return index === 0 ? (secondaryGoalkeeper.has(key) ? 1 : 2) : lowRelevance[index].has(key) ? 2 : 1;
}

export function getPlayerStatisticsCatalog(metrics: PlayerMetric[], position: string, search: string, category: string, unit: string) {
	const normalize = (text: string) => text.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().trim();
	const query = normalize(search);
	return METRIC_DEFINITIONS.map((definition, order) => {
		const metric = metrics.find((item) => item.key === definition.key);
		return { ...definition, unit: getMetricUnit(definition.key), technicalUnit: metric?.unit ?? (definition.key.endsWith("_per_match") ? "PER_MATCH" : definition.key.endsWith("_total") ? "SEASON_TOTAL" : definition.key.endsWith("_percentage") ? "PERCENTAGE" : "AVERAGE_RATING"), value: metric?.value ?? null, order, priority: getMetricPriority(definition.key, position) };
	}).filter((item) => (category === "all" || category === item.category) && (unit === "all" || unit === item.technicalUnit) && (!query || normalize(`${item.key} ${item.label}`).includes(query)))
		.sort((a, b) => {
			const categoryOrder = ["General", "Ataque", "Pases", "Regates", "Duelos", "Defensa", "Portería", "Disciplina"];
			return categoryOrder.indexOf(a.category) - categoryOrder.indexOf(b.category) || Number(a.value === null) - Number(b.value === null) || a.priority - b.priority || a.order - b.order;
		});
}

export function getStatisticsPanels(position: string) {
	const index = getStatisticsPosition(position);
	const groups = [
		[{ title: "Atajadas por zona", keys: ["six_yard_box_saves_per_match", "penalty_area_saves_per_match", "outside_box_saves_per_match"], zones: true }, { title: "Distribución de pases", keys: ["accurate_short_passes_per_match", "inaccurate_short_passes_per_match", "successful_long_balls_per_match", "inaccurate_long_balls_per_match"] }],
		[{ title: "Acciones defensivas", keys: ["tackles_per_match", "interceptions_per_match", "clearances_per_match", "blocked_shots_per_match", "blocked_crosses_per_match", "blocked_passes_per_match"] }, { title: "Duelos aéreos", keys: ["aerial_duels_won_per_match", "aerial_duels_lost_per_match"] }],
		[{ title: "Distribución de pases", keys: ["accurate_short_passes_per_match", "inaccurate_short_passes_per_match", "successful_long_balls_per_match", "inaccurate_long_balls_per_match"] }, { title: "Creación de juego", keys: ["long_key_passes_per_match", "short_key_passes_per_match"] }],
		[{ title: "Tiros por zona", keys: ["six_yard_box_shots_per_match", "penalty_area_shots_per_match", "outside_box_shots_per_match"], zones: true }, { title: "Asistencias por tipo", keys: ["cross_assists_per_match", "corner_assists_per_match", "through_ball_assists_per_match", "free_kick_assists_per_match", "throw_in_assists_per_match", "other_assists_per_match"] }],
	];
	return groups[index] ?? [];
}

export function getStatisticsProfile(position: string) {
	return getStatisticsPosition(position) < 0 ? null : getPositionProfile(position);
}
