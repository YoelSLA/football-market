export interface PortfolioSummaryResponseDTO {
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

export interface PortfolioPositionResponseDTO {
	playerId: number;
	playerName: string | null;
	teamName: string | null;
	position: string | null;
	imageUrl: string | null;
	active: boolean;
	status: "OPEN" | "CLOSED";
	valuationStatus: "VALUED" | "UNVALUED" | "NOT_APPLICABLE";
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

export interface PortfolioPaginationResponseDTO {
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
}

export interface PortfolioResponseDTO {
	summary: PortfolioSummaryResponseDTO;
	positions: PortfolioPositionResponseDTO[];
	pagination: PortfolioPaginationResponseDTO;
}
