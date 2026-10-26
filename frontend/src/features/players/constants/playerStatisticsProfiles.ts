import { METRIC_DEFINITIONS } from "./playerStatisticsCatalog";

export { METRIC_DEFINITIONS };

export interface PositionProfile {
	cards: string[];
	charts: string[];
	additional: string[];
}

export const POSITION_PROFILES: Record<string, PositionProfile> = {
	Goalkeeper: {
		cards: [
			"saves_per_match",
			"pass_completion_percentage",
			"successful_long_balls_per_match",
			"whoscored_rating_average",
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
			"clearances_per_match",
			"aerial_duels_won_per_match",
			"passes_per_match",
			"yellow_cards_total",
			"red_cards_total",
		],
	},
	Defender: {
		cards: [
			"tackles_per_match",
			"interceptions_per_match",
			"clearances_per_match",
			"aerial_duels_won_per_match",
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
			"whoscored_rating_average",
			"pass_completion_percentage",
			"dribbled_past_per_match",
			"attempted_tackles_per_match",
			"fouls_committed_per_match",
			"yellow_cards_total",
			"red_cards_total",
		],
	},
	Midfielder: {
		cards: [
			"passes_per_match",
			"pass_completion_percentage",
			"key_passes_per_match",
			"assists_total",
		],
		charts: [
			"accurate_short_passes_per_match",
			"inaccurate_short_passes_per_match",
			"successful_long_balls_per_match",
			"inaccurate_long_balls_per_match",
			"long_key_passes_per_match",
			"short_key_passes_per_match",
			"tackles_per_match",
			"interceptions_per_match",
		],
		additional: [
			"minutes_played_total",
			"appearances_total",
			"whoscored_rating_average",
			"assists_per_match",
			"successful_dribbles_per_match",
			"tackles_per_match",
			"interceptions_per_match",
			"dispossessed_per_match",
			"bad_controls_per_match",
			"goals_total",
			"fouls_committed_per_match",
		],
	},
	Forward: {
		cards: [
			"goals_total",
			"assists_total",
			"shots_per_match",
			"whoscored_rating_average",
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
			"total_dribbles_per_match",
			"failed_dribbles_per_match",
		],
		additional: [
			"minutes_played_total",
			"appearances_total",
			"goals_per_match",
			"assists_per_match",
			"key_passes_per_match",
			"successful_dribbles_per_match",
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
	if (normalized === "defender" || normalized === "defence" || normalized === "defensa") {
		return POSITION_PROFILES.Defender;
	}
	if (normalized === "midfielder" || normalized === "midfield" || normalized === "mediocampista") {
		return POSITION_PROFILES.Midfielder;
	}
	return POSITION_PROFILES.Forward;
}