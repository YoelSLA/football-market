import type {
	AllocationItemDTO,
	DashboardAllocationDTO,
	DashboardFinancialSummaryDTO,
	DashboardMarketPulseDTO,
	DashboardOverviewResponseDTO,
	DashboardP2PDealDTO,
	DashboardPerformanceResponseDTO,
	DashboardPlayerMoverDTO,
	DashboardRecentActivityDTO,
	DashboardValuePickDTO,
} from "./types/dtos";
import type {
	AllocationItem,
	DashboardOverview,
	FinancialSummary,
	MarketPulse,
	P2PDeal,
	PerformanceSeries,
	PlayerMover,
	PortfolioAllocation,
	RecentActivityItem,
	ValuePick,
} from "./types/models";

function toFinancialSummary(
	dto: DashboardFinancialSummaryDTO,
): FinancialSummary {
	return {
		availableCredits: dto.availableCredits / 100,
		totalPortfolioValue:
			dto.totalPortfolioValue !== null ? dto.totalPortfolioValue / 100 : null,
		netWorth: dto.netWorth !== null ? dto.netWorth / 100 : null,
		totalInvested: dto.totalInvested / 100,
		totalUnrealizedProfitLoss:
			dto.totalUnrealizedProfitLoss !== null
				? dto.totalUnrealizedProfitLoss / 100
				: null,
		totalRealizedProfitLoss: dto.totalRealizedProfitLoss / 100,
		totalProfitLoss:
			dto.totalProfitLoss !== null ? dto.totalProfitLoss / 100 : null,
		totalProfitLossPercentage: dto.totalProfitLossPercentage,
		totalTokens: dto.totalTokens,
		availableTokens: dto.availableTokens,
		reservedTokens: dto.reservedTokens,
		valuationComplete: dto.valuationComplete,
	};
}

function toAllocationItem(dto: AllocationItemDTO): AllocationItem {
	return {
		key: dto.categoryKey,
		label: dto.label,
		totalValue: dto.totalValue / 100,
		percentage: dto.percentage,
		tokenCount: dto.tokenCount,
	};
}

function toPortfolioAllocation(
	dto: DashboardAllocationDTO,
): PortfolioAllocation {
	return {
		byPosition: dto.byPosition.map(toAllocationItem),
		byLeague: dto.byLeague.map(toAllocationItem),
		topHoldingConcentrationPercentage: dto.topHoldingConcentrationPercentage,
		topHoldingPlayerName: dto.topHoldingPlayerName,
	};
}

function toMarketPulse(dto: DashboardMarketPulseDTO): MarketPulse {
	return {
		totalMarketCap: dto.totalMarketCap / 100,
		volume24h: dto.volume24h / 100,
		totalTransactionsCount: dto.totalTransactionsCount,
		activeListingsCount: dto.activeListingsCount,
	};
}

function toPlayerMover(dto: DashboardPlayerMoverDTO): PlayerMover {
	const fullName = `${dto.firstName} ${dto.lastName}`.trim();
	return {
		playerId: dto.playerId,
		name: fullName || "Jugador",
		team: dto.teamName,
		league: dto.leagueName,
		position: dto.position,
		imageUrl: dto.imageUrl,
		currentPrice: dto.currentPrice / 100,
		previousPrice: dto.previousPrice !== null ? dto.previousPrice / 100 : null,
		deltaCredits: dto.deltaCredits !== null ? dto.deltaCredits / 100 : null,
		deltaPercentage: dto.deltaPercentage,
		score: dto.score,
	};
}

function toValuePick(dto: DashboardValuePickDTO): ValuePick {
	const fullName = `${dto.firstName} ${dto.lastName}`.trim();
	return {
		playerId: dto.playerId,
		name: fullName || "Jugador",
		team: dto.teamName,
		league: dto.leagueName,
		position: dto.position,
		imageUrl: dto.imageUrl,
		score: dto.whoScoredScore,
		price: dto.currentPrice / 100,
		valueRatio: dto.valueRatio,
	};
}

function toP2PDeal(dto: DashboardP2PDealDTO): P2PDeal {
	return {
		listingId: dto.listingId,
		playerId: dto.playerId,
		playerName: dto.playerName,
		team: dto.teamName,
		league: dto.leagueName,
		imageUrl: dto.imageUrl,
		availableTokens: dto.availableTokens,
		p2pUnitPrice: dto.p2pUnitPrice / 100,
		officialQuotePrice: dto.officialQuotePrice / 100,
		discountPercentage: dto.discountPercentage,
	};
}

function toRecentActivityItem(
	dto: DashboardRecentActivityDTO,
): RecentActivityItem {
	return {
		id: dto.transactionId,
		type: dto.type,
		marketType: dto.marketType,
		playerId: dto.playerId,
		playerName: dto.playerName,
		tokens: dto.quantity,
		unitPrice: dto.unitPrice / 100,
		totalAmount: dto.totalAmount / 100,
		executedAt: dto.executedAt,
	};
}

export function toDashboardOverview(
	dto: DashboardOverviewResponseDTO,
): DashboardOverview {
	return {
		financialSummary: toFinancialSummary(dto.financialSummary),
		allocation: toPortfolioAllocation(dto.allocation),
		marketPulse: toMarketPulse(dto.marketPulse),
		topGainers: dto.topGainers.map(toPlayerMover),
		topLosers: dto.topLosers.map(toPlayerMover),
		valuePicks: dto.valuePicks.map(toValuePick),
		bestP2PDeals: dto.bestP2PDeals.map(toP2PDeal),
		recentActivity: dto.recentActivity.map(toRecentActivityItem),
		generatedAt: dto.generatedAt,
	};
}

export function toPerformanceSeries(
	dto: DashboardPerformanceResponseDTO,
): PerformanceSeries {
	return {
		dates: dto.dates,
		userPortfolioSeries: dto.userPortfolioSeries.map((v) => v / 100),
		marketBenchmarkSeries: dto.marketBenchmarkSeries.map((v) => v / 100),
	};
}
