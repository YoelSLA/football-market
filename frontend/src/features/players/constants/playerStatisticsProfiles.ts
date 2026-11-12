import {
	Activity,
	CircleDot,
	Footprints,
	Goal,
	type LucideIcon,
	Shield,
	UsersRound,
	Zap,
} from "@/shared/icons";
import { METRIC_DEFINITIONS } from "./playerStatisticsCatalog";

export { METRIC_DEFINITIONS };

export interface MetricSlotConfig {
	key: string;
	fallbackKey?: string;
	subtitle?: string;
}

export interface SummaryPanelConfig {
	title: string;
	subtitle: string;
	icon: LucideIcon;
	metrics: MetricSlotConfig[];
}

export interface SummaryPositionProfile {
	kpis: MetricSlotConfig[];
	panels: SummaryPanelConfig[];
}

export const SUMMARY_POSITION_PROFILES: Record<string, SummaryPositionProfile> =
	{
		Goalkeeper: {
			kpis: [
				{
					key: "whoscored_rating_average",
					fallbackKey: "starts_total",
				},
				{
					key: "appearances_total",
					fallbackKey: "minutes_played_total",
				},
				{
					key: "clearances_per_match",
					fallbackKey: "aerial_duels_won_per_match",
				},
			],
			panels: [
				{
					title: "Atajadas por sector",
					subtitle: "Distribución según zona de disparo",
					icon: Shield,
					metrics: [
						{ key: "penalty_area_saves_per_match" },
						{ key: "outside_box_saves_per_match" },
						{ key: "six_yard_box_saves_per_match" },
					],
				},
				{
					title: "Distribución y juego con los pies",
					subtitle: "Participación y salida de fondo",
					icon: Footprints,
					metrics: [
						{
							key: "passes_per_match",
							fallbackKey: "accurate_short_passes_per_match",
						},
						{
							key: "inaccurate_short_passes_per_match",
							fallbackKey: "bad_controls_per_match",
						},
						{
							key: "inaccurate_long_balls_per_match",
							fallbackKey: "fouls_committed_per_match",
						},
					],
				},
			],
		},
		Defender: {
			kpis: [
				{
					key: "whoscored_rating_average",
					fallbackKey: "starts_total",
				},
				{
					key: "aerial_duels_won_per_match",
					fallbackKey: "total_aerial_duels_per_match",
				},
				{
					key: "pass_completion_percentage",
					fallbackKey: "passes_per_match",
				},
				{
					key: "blocked_shots_per_match",
					fallbackKey: "blocked_crosses_per_match",
				},
			],
			panels: [
				{
					title: "Contención y duelos",
					subtitle: "Acciones de corte e interposición",
					icon: Shield,
					metrics: [
						{ key: "attempted_tackles_per_match" },
						{ key: "blocked_passes_per_match" },
						{ key: "dribbled_past_per_match" },
					],
				},
				{
					title: "Juego aéreo y disciplina",
					subtitle: "Dominio físico y control de faltas",
					icon: UsersRound,
					metrics: [
						{ key: "aerial_duels_lost_per_match" },
						{ key: "fouls_committed_per_match" },
						{
							key: "yellow_cards_total",
							fallbackKey: "red_cards_total",
						},
					],
				},
			],
		},
		Midfielder: {
			kpis: [
				{
					key: "whoscored_rating_average",
					fallbackKey: "starts_total",
				},
				{
					key: "assists_total",
					fallbackKey: "assists_per_match",
				},
				{
					key: "tackles_per_match",
					fallbackKey: "interceptions_per_match",
				},
				{
					key: "successful_dribbles_per_match",
					fallbackKey: "total_dribbles_per_match",
				},
			],
			panels: [
				{
					title: "Construcción y distribución",
					subtitle: "Creación de juego y progresión",
					icon: CircleDot,
					metrics: [
						{ key: "accurate_short_passes_per_match" },
						{ key: "successful_long_balls_per_match" },
						{
							key: "short_key_passes_per_match",
							fallbackKey: "long_key_passes_per_match",
						},
						{ key: "successful_crosses_per_match" },
					],
				},
				{
					title: "Posesión y equilibrio",
					subtitle: "Control técnico y recuperación",
					icon: Activity,
					metrics: [
						{ key: "dispossessed_per_match" },
						{ key: "bad_controls_per_match" },
						{ key: "aerial_duels_won_per_match" },
						{
							key: "fouls_committed_per_match",
							fallbackKey: "yellow_cards_total",
						},
					],
				},
			],
		},
		Forward: {
			kpis: [
				{
					key: "whoscored_rating_average",
					fallbackKey: "starts_total",
				},
				{
					key: "shots_on_target_total",
				},
				{
					key: "goals_per_match",
				},
				{
					key: "successful_dribbles_per_match",
					fallbackKey: "total_dribbles_per_match",
				},
			],
			panels: [
				{
					title: "Definición por zona",
					subtitle: "Presencia y remates hacia portería",
					icon: Goal,
					metrics: [
						{
							key: "penalty_area_shots_per_match",
							fallbackKey: "penalty_area_goals_per_match",
						},
						{
							key: "six_yard_box_shots_per_match",
							fallbackKey: "six_yard_box_goals_per_match",
						},
						{
							key: "outside_box_shots_per_match",
							fallbackKey: "outside_box_goals_per_match",
						},
						{ key: "offsides_per_match" },
					],
				},
				{
					title: "Juego ofensivo y generación",
					subtitle: "Desequilibrio y sociedad en ataque",
					icon: Zap,
					metrics: [
						{
							key: "key_passes_per_match",
							fallbackKey: "assists_per_match",
						},
						{
							key: "aerial_duels_won_per_match",
							fallbackKey: "total_aerial_duels_per_match",
						},
						{
							key: "dispossessed_per_match",
							fallbackKey: "bad_controls_per_match",
						},
						{ key: "successful_crosses_per_match" },
					],
				},
			],
		},
	};

export function getSummaryPositionProfile(
	position: string,
): SummaryPositionProfile | null {
	const normalized = position.trim().toLowerCase();
	if (
		normalized === "goalkeeper" ||
		normalized === "portero" ||
		normalized === "arquero" ||
		normalized === "por" ||
		normalized === "gk"
	) {
		return SUMMARY_POSITION_PROFILES.Goalkeeper;
	}
	if (
		normalized === "defender" ||
		normalized === "defence" ||
		normalized === "defensa" ||
		normalized === "defensor" ||
		normalized === "def"
	) {
		return SUMMARY_POSITION_PROFILES.Defender;
	}
	if (
		normalized === "midfielder" ||
		normalized === "midfield" ||
		normalized === "mediocampista" ||
		normalized === "medio" ||
		normalized === "med"
	) {
		return SUMMARY_POSITION_PROFILES.Midfielder;
	}
	if (
		normalized === "forward" ||
		normalized === "offence" ||
		normalized === "delantero" ||
		normalized === "del" ||
		normalized === "ataque" ||
		normalized === "fw"
	) {
		return SUMMARY_POSITION_PROFILES.Forward;
	}
	return null;
}

export interface PositionProfile {
	cards: string[];
	charts: string[];
	additional: string[];
}

export const POSITION_PROFILES: Record<string, PositionProfile> = {
	Goalkeeper: {
		cards: [
			"whoscored_rating_average",
			"clearances_per_match",
			"aerial_duels_won_per_match",
			"passes_per_match",
		],
		charts: [
			"six_yard_box_saves_per_match",
			"penalty_area_saves_per_match",
			"outside_box_saves_per_match",
			"accurate_short_passes_per_match",
			"inaccurate_short_passes_per_match",
			"successful_long_balls_per_match",
			"inaccurate_long_balls_per_match",
		],
		additional: [
			"minutes_played_total",
			"appearances_total",
			"fouls_committed_per_match",
			"yellow_cards_total",
			"red_cards_total",
		],
	},
	Defender: {
		cards: [
			"whoscored_rating_average",
			"aerial_duels_won_per_match",
			"pass_completion_percentage",
			"blocked_shots_per_match",
		],
		charts: [
			"tackles_per_match",
			"interceptions_per_match",
			"clearances_per_match",
			"blocked_shots_per_match",
			"blocked_crosses_per_match",
			"blocked_passes_per_match",
			"aerial_duels_won_per_match",
			"aerial_duels_lost_per_match",
		],
		additional: [
			"minutes_played_total",
			"appearances_total",
			"dribbled_past_per_match",
			"attempted_tackles_per_match",
			"fouls_committed_per_match",
			"yellow_cards_total",
			"red_cards_total",
		],
	},
	Midfielder: {
		cards: [
			"whoscored_rating_average",
			"assists_total",
			"tackles_per_match",
			"successful_dribbles_per_match",
		],
		charts: [
			"accurate_short_passes_per_match",
			"inaccurate_short_passes_per_match",
			"successful_long_balls_per_match",
			"inaccurate_long_balls_per_match",
			"long_key_passes_per_match",
			"short_key_passes_per_match",
		],
		additional: [
			"minutes_played_total",
			"appearances_total",
			"assists_per_match",
			"interceptions_per_match",
			"dispossessed_per_match",
			"bad_controls_per_match",
			"goals_total",
			"fouls_committed_per_match",
		],
	},
	Forward: {
		cards: [
			"whoscored_rating_average",
			"shots_on_target_total",
			"key_passes_per_match",
			"successful_dribbles_per_match",
		],
		charts: [
			"six_yard_box_shots_per_match",
			"penalty_area_shots_per_match",
			"outside_box_shots_per_match",
			"cross_assists_per_match",
			"corner_assists_per_match",
			"through_ball_assists_per_match",
			"free_kick_assists_per_match",
			"throw_in_assists_per_match",
			"other_assists_per_match",
		],
		additional: [
			"minutes_played_total",
			"appearances_total",
			"goals_per_match",
			"assists_per_match",
			"aerial_duels_won_per_match",
			"offsides_per_match",
			"pass_completion_percentage",
			"yellow_cards_total",
			"red_cards_total",
		],
	},
};

export function getPositionProfile(position: string): PositionProfile {
	const normalized = position.toLowerCase();
	if (normalized === "goalkeeper" || normalized === "portero") {
		return POSITION_PROFILES.Goalkeeper;
	}
	if (
		normalized === "defender" ||
		normalized === "defence" ||
		normalized === "defensa"
	) {
		return POSITION_PROFILES.Defender;
	}
	if (
		normalized === "midfielder" ||
		normalized === "midfield" ||
		normalized === "mediocampista"
	) {
		return POSITION_PROFILES.Midfielder;
	}
	return POSITION_PROFILES.Forward;
}
