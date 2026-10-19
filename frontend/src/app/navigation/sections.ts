import type { NavSection } from "@/shared/components";

/**
 * Secciones de la navegacion principal.
 *
 * La configuracion vive en `app/` porque es composicion global: el Navbar
 * la recibe por props y no conoce rutas ni pantallas.
 *
 * Una seccion o pestana con `path: null` es una funcionalidad futura:
 * se muestra deshabilitada, rotulada "Proximamente", sin ruta ni
 * navegacion. El inventario solo habilita lo que ya existe y funciona.
 */
export const NAV_SECTIONS: NavSection[] = [
	{
		key: "home",
		label: "Inicio",
		icon: "home",
		path: "/home",
		tabs: [{ key: "home-dashboard", label: "Dashboard", path: "/home" }],
	},
	{
		key: "players",
		label: "Jugadores",
		icon: "players",
		path: "/players",
		tabs: [
			{ key: "players-catalog", label: "Catálogo", path: "/players" },
			{ key: "players-ranking", label: "Ranking", path: null },
			{
				key: "players-quote-history",
				label: "Historial de cotizaciones",
				path: null,
			},
		],
	},
	{
		key: "market",
		label: "Mercado",
		icon: "market",
		path: null,
		tabs: [
			{ key: "market-buy", label: "Comprar tokens", path: null },
			{ key: "market-offers", label: "Ofertas disponibles", path: null },
			{ key: "market-listings", label: "Mis publicaciones", path: null },
		],
	},
	{
		key: "portfolio",
		label: "Cartera",
		icon: "portfolio",
		path: null,
		tabs: [
			{ key: "portfolio-overview", label: "Mi portfolio", path: null },
			{ key: "portfolio-holdings", label: "Mis tenencias", path: null },
			{
				key: "portfolio-transactions",
				label: "Historial de transacciones",
				path: null,
			},
			{ key: "portfolio-stats", label: "Estadísticas/gráficos", path: null },
		],
	},
];
