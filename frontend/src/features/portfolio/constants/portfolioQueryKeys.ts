import type {
	PositionFilter,
	TransactionHistoryQueryParams,
} from "@/features/portfolio/types";

export const portfolioQueryKeys = {
	summary: ["portfolio", "summary"] as const,
	transactions: (params: TransactionHistoryQueryParams) =>
		["portfolio", "transactions", params] as const,
	holdings: (status: PositionFilter, page: number, pageSize: number) =>
		["portfolio", "holdings", status, page, pageSize] as const,
};
