import {
	ArrowRightFromLine,
	ArrowRightLeft,
	ArrowUpRight,
	Box,
	BrickWall,
	CalendarDays,
	CheckCheck,
	ChevronsDown,
	ChevronsUp,
	CircleDot,
	CircleSlash,
	CircleX,
	Clock,
	CornerUpRight,
	Crosshair,
	FastForward,
	Flag,
	FlagTriangleLeft,
	FlagTriangleRight,
	Flame,
	Footprints,
	Goal,
	Hand,
	HandFist,
	Handshake,
	IdCard,
	Key,
	KeyRound,
	KeySquare,
	Layers3,
	LocateFixed,
	type LucideIcon,
	Maximize2,
	MoreHorizontal,
	Navigation,
	Percent,
	Radar,
	Route,
	Scissors,
	Send,
	Shield,
	ShieldAlert,
	ShieldBan,
	ShieldCheck,
	ShieldMinus,
	ShieldOff,
	ShieldX,
	Shuffle,
	Sparkles,
	Spline,
	SportShoe,
	Square,
	SquareDot,
	SquareStack,
	SquareX,
	Star,
	Swords,
	Target,
	Telescope,
	TriangleAlert,
	UserCheck,
	UserMinus,
	UsersRound,
	Whistle,
	Zap,
	ZapOff,
} from "@/shared/icons";

export const CANONICAL_METRIC_KEYS = [
	// General (5)
	"minutes_played_total",
	"appearances_total",
	"starts_total",
	"substitute_appearances_total",
	"whoscored_rating_average",

	// Ataque (19)
	"goals_total",
	"assists_total",
	"shots_per_match",
	"shots_on_target_total",
	"goals_per_match",
	"assists_per_match",
	"six_yard_box_shots_per_match",
	"penalty_area_shots_per_match",
	"outside_box_shots_per_match",
	"six_yard_box_goals_per_match",
	"penalty_area_goals_per_match",
	"outside_box_goals_per_match",
	"cross_assists_per_match",
	"corner_assists_per_match",
	"through_ball_assists_per_match",
	"free_kick_assists_per_match",
	"throw_in_assists_per_match",
	"other_assists_per_match",
	"offsides_per_match",

	// Pases (10)
	"pass_completion_percentage",
	"passes_per_match",
	"successful_long_balls_per_match",
	"inaccurate_long_balls_per_match",
	"accurate_short_passes_per_match",
	"inaccurate_short_passes_per_match",
	"key_passes_per_match",
	"long_key_passes_per_match",
	"short_key_passes_per_match",
	"successful_crosses_per_match",

	// Regates (5)
	"successful_dribbles_per_match",
	"failed_dribbles_per_match",
	"total_dribbles_per_match",
	"dispossessed_per_match",
	"bad_controls_per_match",

	// Duelos (3)
	"aerial_duels_won_per_match",
	"aerial_duels_lost_per_match",
	"total_aerial_duels_per_match",

	// Defensa (10)
	"tackles_per_match",
	"interceptions_per_match",
	"clearances_per_match",
	"blocked_shots_per_match",
	"dribbled_past_per_match",
	"offsides_won_per_match",
	"own_goals_total",
	"attempted_tackles_per_match",
	"blocked_crosses_per_match",
	"blocked_passes_per_match",

	// Portería (4)
	"saves_per_match",
	"six_yard_box_saves_per_match",
	"penalty_area_saves_per_match",
	"outside_box_saves_per_match",

	// Disciplina (4)
	"yellow_cards_total",
	"red_cards_total",
	"second_yellow_total",
	"fouls_committed_per_match",
] as const;

export type CanonicalMetricKey = (typeof CANONICAL_METRIC_KEYS)[number];

export interface MetricDefinition {
	key: CanonicalMetricKey;
	label: string;
	category: string;
	unit: string;
}

export const METRIC_DEFINITIONS: readonly MetricDefinition[] = [
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

export const METRIC_ICONS: Record<CanonicalMetricKey, LucideIcon> = {
	// General (5)
	minutes_played_total: Clock,
	appearances_total: CalendarDays,
	starts_total: UserCheck,
	substitute_appearances_total: ArrowRightLeft,
	whoscored_rating_average: Star,

	// Ataque (19)
	goals_total: Goal,
	assists_total: Handshake,
	shots_per_match: Crosshair,
	shots_on_target_total: Target,
	goals_per_match: Flame,
	assists_per_match: Send,
	six_yard_box_shots_per_match: SquareDot,
	penalty_area_shots_per_match: Box,
	outside_box_shots_per_match: Telescope,
	six_yard_box_goals_per_match: LocateFixed,
	penalty_area_goals_per_match: CircleDot,
	outside_box_goals_per_match: Sparkles,
	cross_assists_per_match: CornerUpRight,
	corner_assists_per_match: FlagTriangleRight,
	through_ball_assists_per_match: ArrowUpRight,
	free_kick_assists_per_match: Spline,
	throw_in_assists_per_match: ArrowRightFromLine,
	other_assists_per_match: MoreHorizontal,
	offsides_per_match: Flag,

	// Pases (10)
	pass_completion_percentage: Percent,
	passes_per_match: Route,
	successful_long_balls_per_match: FastForward,
	inaccurate_long_balls_per_match: CircleSlash,
	accurate_short_passes_per_match: CheckCheck,
	inaccurate_short_passes_per_match: CircleX,
	key_passes_per_match: Key,
	long_key_passes_per_match: KeyRound,
	short_key_passes_per_match: KeySquare,
	successful_crosses_per_match: Navigation,

	// Regates (5)
	successful_dribbles_per_match: Zap,
	failed_dribbles_per_match: ZapOff,
	total_dribbles_per_match: Shuffle,
	dispossessed_per_match: UserMinus,
	bad_controls_per_match: TriangleAlert,

	// Duelos (3)
	aerial_duels_won_per_match: ChevronsUp,
	aerial_duels_lost_per_match: ChevronsDown,
	total_aerial_duels_per_match: Swords,

	// Defensa (10)
	tackles_per_match: Scissors,
	interceptions_per_match: Radar,
	clearances_per_match: ShieldBan,
	blocked_shots_per_match: BrickWall,
	dribbled_past_per_match: ShieldOff,
	offsides_won_per_match: FlagTriangleLeft,
	own_goals_total: ShieldAlert,
	attempted_tackles_per_match: SportShoe,
	blocked_crosses_per_match: ShieldX,
	blocked_passes_per_match: ShieldMinus,

	// Portería (4)
	saves_per_match: Hand,
	six_yard_box_saves_per_match: HandFist,
	penalty_area_saves_per_match: ShieldCheck,
	outside_box_saves_per_match: Maximize2,

	// Disciplina (4)
	yellow_cards_total: Square,
	red_cards_total: SquareX,
	second_yellow_total: SquareStack,
	fouls_committed_per_match: Whistle,
};

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

export const CATEGORY_ICONS: Record<string, LucideIcon> = {
	all: Layers3,
	General: Clock,
	Ataque: Goal,
	Pases: Footprints,
	Regates: Zap,
	Duelos: UsersRound,
	Defensa: Shield,
	Portería: CircleDot,
	Disciplina: IdCard,
};
