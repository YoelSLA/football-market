import type { StatisticsTab } from "@/features/players/types";
import { BarChart3, List, TrendingUp } from "@/shared/icons";

export interface PlayerDetailTabItem {
	key: StatisticsTab;
	label: string;
	Icon: typeof TrendingUp;
}

export const PLAYER_DETAIL_TABS: readonly PlayerDetailTabItem[] = [
	{ key: "quotes", label: "Cotizaciones", Icon: TrendingUp },
	{ key: "summary", label: "Resumen deportivo", Icon: BarChart3 },
	{ key: "catalog", label: "Todas las estadísticas", Icon: List },
] as const;
