import type { StatisticsTab } from "@/features/players/types";
import { BarChart3, List, TrendingUp } from "@/shared/icons";

export interface PlayerDetailTabItem {
	key: StatisticsTab;
	label: string;
	Icon: typeof TrendingUp;
}

export const PLAYER_DETAIL_TABS: readonly PlayerDetailTabItem[] = [
	{ key: "quotes", label: "Cotización & Mercado", Icon: TrendingUp },
	{ key: "summary", label: "Resumen Deportivo & Radar", Icon: BarChart3 },
	{ key: "catalog", label: "60 Estadísticas Completas", Icon: List },
] as const;
