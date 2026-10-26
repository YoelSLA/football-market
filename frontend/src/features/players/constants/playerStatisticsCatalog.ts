export const METRIC_DEFINITIONS = [
	// General (5)
	{
		key: "minutes_played_total",
		label: "Minutos jugados",
		category: "General",
		unit: "minutos",
	},
	{
		key: "appearances_total",
		label: "Apariciones",
		category: "General",
		unit: "partidos",
	},
	{
		key: "starts_total",
		label: "Titularidades",
		category: "General",
		unit: "partidos",
	},
	{
		key: "substitute_appearances_total",
		label: "Apariciones como suplente",
		category: "General",
		unit: "partidos",
	},
	{
		key: "whoscored_rating_average",
		label: "Calificación WhoScored",
		category: "General",
		unit: "puntos",
	},

	// Ataque (19)
	{ key: "goals_total", label: "Goles", category: "Ataque", unit: "goles" },
	{
		key: "assists_total",
		label: "Asistencias",
		category: "Ataque",
		unit: "asistencias",
	},
	{
		key: "shots_per_match",
		label: "Tiros por partido",
		category: "Ataque",
		unit: "tiros",
	},
	{
		key: "shots_on_target_total",
		label: "Tiros al arco",
		category: "Ataque",
		unit: "tiros",
	},
	{
		key: "goals_per_match",
		label: "Goles por partido",
		category: "Ataque",
		unit: "goles",
	},
	{
		key: "assists_per_match",
		label: "Asistencias por partido",
		category: "Ataque",
		unit: "asistencias",
	},
	{
		key: "six_yard_box_shots_per_match",
		label: "Tiros desde el área chica",
		category: "Ataque",
		unit: "tiros",
	},
	{
		key: "penalty_area_shots_per_match",
		label: "Tiros desde el área penal",
		category: "Ataque",
		unit: "tiros",
	},
	{
		key: "outside_box_shots_per_match",
		label: "Tiros desde fuera del área",
		category: "Ataque",
		unit: "tiros",
	},
	{
		key: "six_yard_box_goals_per_match",
		label: "Goles desde el área chica",
		category: "Ataque",
		unit: "goles",
	},
	{
		key: "penalty_area_goals_per_match",
		label: "Goles desde el área penal",
		category: "Ataque",
		unit: "goles",
	},
	{
		key: "outside_box_goals_per_match",
		label: "Goles desde fuera del área",
		category: "Ataque",
		unit: "goles",
	},
	{
		key: "cross_assists_per_match",
		label: "Asistencias de centro",
		category: "Ataque",
		unit: "asistencias",
	},
	{
		key: "corner_assists_per_match",
		label: "Asistencias de córner",
		category: "Ataque",
		unit: "asistencias",
	},
	{
		key: "through_ball_assists_per_match",
		label: "Asistencias de pase filtrado",
		category: "Ataque",
		unit: "asistencias",
	},
	{
		key: "free_kick_assists_per_match",
		label: "Asistencias de tiro libre",
		category: "Ataque",
		unit: "asistencias",
	},
	{
		key: "throw_in_assists_per_match",
		label: "Asistencias de saque lateral",
		category: "Ataque",
		unit: "asistencias",
	},
	{
		key: "other_assists_per_match",
		label: "Otras asistencias",
		category: "Ataque",
		unit: "asistencias",
	},
	{
		key: "offsides_per_match",
		label: "Fueras de juego",
		category: "Ataque",
		unit: "veces",
	},

	// Pases (10)
	{
		key: "pass_completion_percentage",
		label: "Precisión de pases",
		category: "Pases",
		unit: "%",
	},
	{
		key: "passes_per_match",
		label: "Pases por partido",
		category: "Pases",
		unit: "pases",
	},
	{
		key: "successful_long_balls_per_match",
		label: "Pases largos acertados",
		category: "Pases",
		unit: "pases",
	},
	{
		key: "inaccurate_long_balls_per_match",
		label: "Pases largos fallidos",
		category: "Pases",
		unit: "pases",
	},
	{
		key: "accurate_short_passes_per_match",
		label: "Pases cortos acertados",
		category: "Pases",
		unit: "pases",
	},
	{
		key: "inaccurate_short_passes_per_match",
		label: "Pases cortos fallidos",
		category: "Pases",
		unit: "pases",
	},
	{
		key: "key_passes_per_match",
		label: "Pases clave",
		category: "Pases",
		unit: "pases",
	},
	{
		key: "long_key_passes_per_match",
		label: "Pases clave largos",
		category: "Pases",
		unit: "pases",
	},
	{
		key: "short_key_passes_per_match",
		label: "Pases clave cortos",
		category: "Pases",
		unit: "pases",
	},
	{
		key: "successful_crosses_per_match",
		label: "Centros acertados",
		category: "Pases",
		unit: "centros",
	},

	// Regates (5)
	{
		key: "successful_dribbles_per_match",
		label: "Regates exitosos",
		category: "Regates",
		unit: "regates",
	},
	{
		key: "failed_dribbles_per_match",
		label: "Regates fallidos",
		category: "Regates",
		unit: "regates",
	},
	{
		key: "total_dribbles_per_match",
		label: "Regates intentados",
		category: "Regates",
		unit: "regates",
	},
	{
		key: "dispossessed_per_match",
		label: "Pérdidas por desposesión",
		category: "Regates",
		unit: "veces",
	},
	{
		key: "bad_controls_per_match",
		label: "Malos controles",
		category: "Regates",
		unit: "veces",
	},

	// Duelos (3)
	{
		key: "aerial_duels_won_per_match",
		label: "Duelos aéreos ganados",
		category: "Duelos",
		unit: "duelos",
	},
	{
		key: "aerial_duels_lost_per_match",
		label: "Duelos aéreos perdidos",
		category: "Duelos",
		unit: "duelos",
	},
	{
		key: "total_aerial_duels_per_match",
		label: "Duelos aéreos disputados",
		category: "Duelos",
		unit: "duelos",
	},

	// Defensa (10)
	{
		key: "tackles_per_match",
		label: "Entradas",
		category: "Defensa",
		unit: "entradas",
	},
	{
		key: "interceptions_per_match",
		label: "Intercepciones",
		category: "Defensa",
		unit: "intercepciones",
	},
	{
		key: "clearances_per_match",
		label: "Despejes",
		category: "Defensa",
		unit: "despejes",
	},
	{
		key: "blocked_shots_per_match",
		label: "Tiros bloqueados",
		category: "Defensa",
		unit: "bloqueos",
	},
	{
		key: "dribbled_past_per_match",
		label: "Veces superado por regate",
		category: "Defensa",
		unit: "veces",
	},
	{
		key: "offsides_won_per_match",
		label: "Fueras de juego provocados",
		category: "Defensa",
		unit: "veces",
	},
	{
		key: "own_goals_total",
		label: "Autogoles",
		category: "Defensa",
		unit: "goles",
	},
	{
		key: "attempted_tackles_per_match",
		label: "Entradas intentadas",
		category: "Defensa",
		unit: "entradas",
	},
	{
		key: "blocked_crosses_per_match",
		label: "Centros bloqueados",
		category: "Defensa",
		unit: "bloqueos",
	},
	{
		key: "blocked_passes_per_match",
		label: "Pases bloqueados",
		category: "Defensa",
		unit: "bloqueos",
	},

	// Portería (4)
	{
		key: "saves_per_match",
		label: "Atajadas",
		category: "Portería",
		unit: "atajadas",
	},
	{
		key: "six_yard_box_saves_per_match",
		label: "Atajadas en el área chica",
		category: "Portería",
		unit: "atajadas",
	},
	{
		key: "penalty_area_saves_per_match",
		label: "Atajadas en el área penal",
		category: "Portería",
		unit: "atajadas",
	},
	{
		key: "outside_box_saves_per_match",
		label: "Atajadas fuera del área",
		category: "Portería",
		unit: "atajadas",
	},

	// Disciplina (4)
	{
		key: "yellow_cards_total",
		label: "Tarjetas amarillas",
		category: "Disciplina",
		unit: "tarjetas",
	},
	{
		key: "red_cards_total",
		label: "Tarjetas rojas",
		category: "Disciplina",
		unit: "tarjetas",
	},
	{
		key: "second_yellow_total",
		label: "Segundas amarillas",
		category: "Disciplina",
		unit: "tarjetas",
	},
	{
		key: "fouls_committed_per_match",
		label: "Faltas cometidas",
		category: "Disciplina",
		unit: "faltas",
	},
];

export const CATEGORIES = [
	"General",
	"Ataque",
	"Pases",
	"Regates",
	"Duelos",
	"Defensa",
	"Portería",
	"Disciplina",
] as const;

export const CATEGORY_ORDER: Record<string, number> = {
	General: 1,
	Ataque: 2,
	Pases: 3,
	Regates: 4,
	Duelos: 5,
	Defensa: 6,
	Portería: 7,
	Disciplina: 8,
};
