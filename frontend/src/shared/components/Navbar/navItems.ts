export const NAV_ITEMS = [
	{
		key: "home",
		label: "Inicio",
		to: "/home",
	},
	{
		key: "players",
		label: "Jugadores",
		to: "/players",
	},
] as const;

export type NavSection = (typeof NAV_ITEMS)[number]["key"];
