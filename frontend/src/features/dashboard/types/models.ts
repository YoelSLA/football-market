/**
 * Modelos de dominio UI en el frontend (valores monetarios convertidos a créditos).
 */

export interface FinancialSummary {
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

export interface AllocationItem {
	key: string;
	label: string;
	totalValue: number;
	percentage: number;
	tokenCount: number;
}

export interface PortfolioAllocation {
	byPosition: AllocationItem[];
	byLeague: AllocationItem[];
	topHoldingConcentrationPercentage: number;
	topHoldingPlayerName: string;
}

export interface MarketPulse {
	totalMarketCap: number;
	volume24h: number;
	totalTransactionsCount: number;
	activeListingsCount: number;
}

export interface PlayerMover {
	playerId: number;
	name: string;
	team: string;
	league: string;
	position: string;
	imageUrl?: string | null;
	currentPrice: number;
	previousPrice: number | null;
	deltaCredits: number | null;
	deltaPercentage: number | null;
	score?: number | null;
}

export interface ValuePick {
	playerId: number;
	name: string;
	team: string;
	league: string;
	position: string;
	imageUrl?: string | null;
	score: number;
	price: number;
	valueRatio: number;
}

export interface P2PDeal {
	listingId: number;
	playerId: number;
	playerName: string;
	team: string;
	league: string;
	imageUrl?: string | null;
	availableTokens: number;
	p2pUnitPrice: number;
	officialQuotePrice: number;
	discountPercentage: number;
}

export interface RecentActivityItem {
	id: number;
	type: "BUY" | "SELL";
	marketType: "OFFICIAL" | "P2P";
	playerId: number;
	playerName: string;
	tokens: number;
	unitPrice: number;
	totalAmount: number;
	executedAt: string;
}

export interface DashboardOverview {
	financialSummary: FinancialSummary;
	allocation: PortfolioAllocation;
	marketPulse: MarketPulse;
	topGainers: PlayerMover[];
	topLosers: PlayerMover[];
	valuePicks: ValuePick[];
	bestP2PDeals: P2PDeal[];
	recentActivity: RecentActivityItem[];
	generatedAt: string;
}

export interface PerformanceSeries {
	dates: string[];
	userPortfolioSeries: number[];
	marketBenchmarkSeries: number[];
}
