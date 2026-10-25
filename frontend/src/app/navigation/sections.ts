import type { NavSection } from "@/shared/components";
import {
	BarChart3,
	FileText,
	History,
	Package,
	Puzzle,
	ShoppingCart,
	Tag,
} from "@/shared/icons";

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
		path: "/market/buy",
		tabs: [
			{
				key: "market-buy",
				label: "Comprar tokens",
				icon: ShoppingCart,
				path: "/market/buy",
			},
			{ key: "market-sell", label: "Vender tokens", icon: Tag, path: null },
			{ key: "market-orders", label: "Mis órdenes", icon: Puzzle, path: null },
			{
				key: "market-history",
				label: "Historial de operaciones",
				icon: History,
				path: null,
			},
		],
	},
	{
		key: "portfolio",
		label: "Cartera",
		icon: "portfolio",
		path: "/portfolio",
		tabs: [
			{
				key: "portfolio-overview",
				label: "Mi portfolio",
				path: "/portfolio",
				icon: BarChart3,
			},
			{
				key: "portfolio-holdings",
				label: "Mis tenencias",
				path: "/portfolio/holdings",
				icon: Package,
			},
			{
				key: "portfolio-transactions",
				icon: FileText,
				badgeTone: "gold",
				label: "Historial de transacciones",
				path: null,
			},
			{
				key: "portfolio-stats",
				label: "Estadísticas/gráficos",
				path: null,
				icon: BarChart3,
				badgeTone: "violet",
			},
		],
	},
];
