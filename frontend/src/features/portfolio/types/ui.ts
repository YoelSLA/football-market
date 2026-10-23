import type { ReactNode } from "react";
import type { LucideIcon } from "@/shared/icons";

export interface PortfolioValueRowData {
	label: string;
	value: ReactNode;
	result?: number | null;
	icon?: LucideIcon;
	iconTone?: "cyan" | "violet" | "green" | "gold";
	iconTile?: boolean;
	emphasis?: boolean;
}

export type PortfolioMetricIcon = "value" | "invested" | "profit" | "positions";

export type PortfolioMetricTone =
	| "current"
	| "invested"
	| "profit"
	| "positions";
