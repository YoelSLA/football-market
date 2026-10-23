import type { PositionFilter } from "@/features/portfolio/types";

export const portfolioQueryKeys = {
	summary: ["portfolio", "summary"] as const,
	holdings: (status: PositionFilter, page: number, pageSize: number) =>
		["portfolio", "holdings", status, page, pageSize] as const,
};
