import type { NavSection } from "@/shared/components";
import {
	BarChart3,
	FileText,
	LayoutDashboard,
	Package,
	Puzzle,
	ShoppingCart,
	Tag,
	Trophy,
	UsersRound,
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
		tabs: [
			{
				key: "home-dashboard",
				label: "Dashboard",
				icon: LayoutDashboard,
				path: "/home",
			},
		],
	},
	{
		key: "players",
		label: "Jugadores",
		icon: "players",
		path: "/players",
		tabs: [
			{
				key: "players-catalog",
				label: "Catálogo",
				icon: UsersRound,
				path: "/players",
			},
			{
				key: "players-ranking",
				label: "Ranking",
				icon: Trophy,
				path: "/players/ranking",
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
				key: "market-official",
				label: "Mercado Oficial",
				icon: ShoppingCart,
				path: "/market/buy",
			},
			{
				key: "market-p2p",
				label: "Mercado P2P",
				icon: UsersRound,
				path: "/market/p2p",
			},
			{
				key: "market-sell",
				label: "Vender tokens",
				icon: Tag,
				path: "/market/sell",
			},
			{
				key: "market-orders",
				label: "Mis órdenes",
				icon: Puzzle,
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
				icon: BarChart3,
				path: "/portfolio",
			},
			{
				key: "portfolio-holdings",
				label: "Mis tenencias",
				icon: Package,
				path: "/portfolio/holdings",
			},
			{
				key: "portfolio-transactions",
				icon: FileText,
				badgeTone: "gold",
				label: "Historial de transacciones",
				path: "/portfolio/transactions",
			},
		],
	},
];
