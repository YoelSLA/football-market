export type PositionStatus = "OPEN" | "CLOSED";
export type ValuationStatus = "VALUED" | "UNVALUED" | "NOT_APPLICABLE";
export type PositionFilter = "ALL" | PositionStatus;

/** Importes en centésimas; porcentajes en puntos porcentuales. */
export interface PortfolioSummary {
	totalPositions: number;
	totalTokens: number;
	totalReservedTokens: number;
	totalAvailableTokens: number;
	totalInvested: number;
	totalCurrentValue: number | null;
	totalRealizedProfitLoss: number;
	totalUnrealizedProfitLoss: number | null;
	totalProfitLoss: number | null;
	totalProfitLossPercentage: number | null;
	totalBuyTransactions: number;
	totalSellTransactions: number;
	valuationComplete: boolean;
}

export interface PortfolioPosition {
	playerId: number;
	playerName: string | null;
	teamName: string | null;
	position: string | null;
	imageUrl: string | null;
	active: boolean;
	status: PositionStatus;
	valuationStatus: ValuationStatus;
	quantity: number;
	reservedQuantity: number;
	availableQuantity: number;
	acquisitionCost: number;
	averageUnitPrice: number | null;
	currentUnitPrice: number | null;
	priceUpdatedAt: string | null;
	currentValue: number | null;
	realizedProfitLoss: number;
	unrealizedProfitLoss: number | null;
	totalProfitLoss: number | null;
	unrealizedProfitLossPercentage: number | null;
	portfolioWeightPercentage: number | null;
	openedAt: string;
	closedAt: string | null;
	lastTransactionAt: string | null;
	totalBuyTransactions: number;
	totalSellTransactions: number;
}

export interface PortfolioPagination {
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
}

export interface Portfolio {
	summary: PortfolioSummary;
	positions: PortfolioPosition[];
	pagination: PortfolioPagination;
}

export interface PortfolioQueryParams {
	page: number;
	size: number;
	status?: PositionStatus;
}
