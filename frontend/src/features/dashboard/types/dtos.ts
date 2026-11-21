/**
 * Contratos de respuesta HTTP del backend (valores monetarios en centésimas de crédito).
 */

export interface DashboardFinancialSummaryDTO {
	availableCredits: number;
	totalPortfolioValue: number | null;
	netWorth: number | null;
	totalInvested: number;
	totalUnrealizedProfitLoss: number | null;
	totalRealizedProfitLoss: number;
	totalProfitLoss: number | null;
	totalProfitLossPercentage: number | null;
	totalTokens: number;
	availableTokens: number;
	reservedTokens: number;
	valuationComplete: boolean;
}

export interface AllocationItemDTO {
	categoryKey: string;
	label: string;
	totalValue: number;
	percentage: number;
	tokenCount: number;
}

export interface DashboardAllocationDTO {
	byPosition: AllocationItemDTO[];
	byLeague: AllocationItemDTO[];
	topHoldingConcentrationPercentage: number;
	topHoldingPlayerName: string;
}

export interface DashboardMarketPulseDTO {
	totalMarketCap: number;
	volume24h: number;
	totalTransactionsCount: number;
	activeListingsCount: number;
}

export interface DashboardPlayerMoverDTO {
	playerId: number;
	firstName: string;
	lastName: string;
	teamName: string;
	leagueName: string;
	position: string;
	imageUrl?: string | null;
	currentPrice: number;
	previousPrice: number | null;
	deltaCredits: number | null;
	deltaPercentage: number | null;
	score?: number | null;
}

export interface DashboardValuePickDTO {
	playerId: number;
	firstName: string;
	lastName: string;
	teamName: string;
	leagueName: string;
	position: string;
	imageUrl?: string | null;
	whoScoredScore: number;
	currentPrice: number;
	valueRatio: number;
}

export interface DashboardP2PDealDTO {
	listingId: number;
	playerId: number;
	playerName: string;
	teamName: string;
	leagueName: string;
	imageUrl?: string | null;
	availableTokens: number;
	p2pUnitPrice: number;
	officialQuotePrice: number;
	discountPercentage: number;
}

export interface DashboardRecentActivityDTO {
	transactionId: number;
	type: "BUY" | "SELL";
	marketType: "OFFICIAL" | "P2P";
	playerId: number;
	playerName: string;
	quantity: number;
	unitPrice: number;
	totalAmount: number;
	executedAt: string;
}

export interface DashboardOverviewResponseDTO {
	financialSummary: DashboardFinancialSummaryDTO;
	allocation: DashboardAllocationDTO;
	marketPulse: DashboardMarketPulseDTO;
	topGainers: DashboardPlayerMoverDTO[];
	topLosers: DashboardPlayerMoverDTO[];
	valuePicks: DashboardValuePickDTO[];
	bestP2PDeals: DashboardP2PDealDTO[];
	recentActivity: DashboardRecentActivityDTO[];
	generatedAt: string;
}

export interface DashboardPerformanceResponseDTO {
	dates: string[];
	userPortfolioSeries: number[];
	marketBenchmarkSeries: number[];
}
