/**
 * DTOs de lectura de cotización y orden de compra de tokens (SPEC-019).
 * Utilizados exclusivamente dentro de la frontera de Service y Mapper.
 */

export interface QuoteResponseDTO {
	price: number;
	effectiveDate: string;
	calculatedAt: string;
	id?: number;
	playerId?: number;
	previousPrice?: number | null;
}

export interface OrderBuyRequestDTO {
	playerId: number;
	quantity: number;
}

export interface OrderBuyResponseDTO {
	transactionId: number;
	playerId: number;
	quantity: number;
	unitPrice: number;
	totalAmount: number;
	executedAt: string;
	listingId?: number;
}

export interface BuyListingRequestDTO {
	listingId: number;
}

export interface TokenListingDTO {
	listingId: number;
	sellerId: number;
	playerId: number;
	quantity: number;
	unitPrice: number;
	totalAmount: number;
	status: "ACTIVE";
	createdAt: string;
	expiresAt: string;
}

export interface TokenListingsDTO {
	summary?: UserOrdersSummaryDTO | null;
	listings: TokenListingDTO[];
	pagination?: MarketPaginationResponseDTO | null;
}

export interface UserOrdersSummaryDTO {
	activeOrders: number;
	activeTokens: number;
	activeValue: number;
	soldOrders: number;
	soldTokens: number;
	soldValue: number;
	cancelledOrders: number;
	expiredOrders: number;
	totalOrders: number;
}

export interface MarketPaginationResponseDTO {
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
}

export interface UserOrderListingDTO {
	listingId: number;
	sellerId: number;
	playerId: number;
	quantity: number;
	unitPrice: number;
	totalAmount: number;
	status: "ACTIVE" | "SOLD" | "CANCELLED" | "EXPIRED";
	createdAt: string;
	expiresAt: string;
	playerName?: string | null;
	teamId?: number | null;
	teamName?: string | null;
	leagueId?: number | null;
	leagueName?: string | null;
	position?: string | null;
	imageUrl?: string | null;
	fallbackImageUrl?: string | null;
}

export interface UserOrdersPageResponseDTO {
	summary: UserOrdersSummaryDTO | null;
	listings: UserOrderListingDTO[];
	pagination: MarketPaginationResponseDTO | null;
}

export interface CancelListingResponseDTO {
	listingId: number;
	sellerId: number;
	playerId: number;
	quantity: number;
	unitPrice: number;
	totalAmount: number;
	status: "CANCELLED";
	createdAt: string;
	expiresAt: string;
	closedAt: string | null;
}

export interface OfficialMarketQuoteResponseDTO {
	price: number;
	previousPrice: number | null;
	variationPercentage: number | null;
	performanceScore: number;
	multiplier: number;
	calculationOrigin: string;
	effectiveDate: string;
	calculatedAt: string;
}

export interface OfficialMarketPlayerResponseDTO {
	id: number;
	name: string;
	position: string;
	nationality: string | null;
	teamId: number | null;
	teamName: string | null;
	teamBadgeUrl: string | null;
	leagueId: number | null;
	leagueName: string | null;
	imageUrl: string | null;
	quote: OfficialMarketQuoteResponseDTO | null;
}

export interface OfficialMarketPageResponseDTO {
	content: OfficialMarketPlayerResponseDTO[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
	priceRange: {
		min: number | null;
		max: number | null;
	};
}

export interface OrderSellRequestDTO {
	playerId: number;
	quantity: number;
	unitPrice?: number;
}

export interface OrderSellOfficialRequestDTO {
	playerId: number;
	quantity: number;
}

export interface OrderSellOfficialResponseDTO {
	transactionId: number;
	buyerId: number;
	sellerId: number;
	playerId: number;
	quantity: number;
	quotePrice: number;
	discountPercentage: number;
	unitPrice: number;
	totalAmount: number;
	executedAt: string;
}

export interface OrderSellResponseDTO {
	listingId: number;
	playerId: number;
	quantity: number;
	unitPrice: number;
	totalAmount: number;
	status: string;
	createdAt: string;
	expiresAt: string;
}

export interface P2PMarketPlayerResponseDTO {
	id: number;
	name: string;
	position: string;
	nationality: string | null;
	teamId: number | null;
	teamName: string | null;
	teamBadgeUrl: string | null;
	leagueId: number | null;
	leagueName: string | null;
	imageUrl: string | null;
	listingCount: number;
	availableQuantity: number;
	minimumUnitPrice: number;
}

export interface P2PMarketPageResponseDTO {
	content: P2PMarketPlayerResponseDTO[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
	priceRange: {
		min: number | null;
		max: number | null;
	};
}

export type MarketBiasDTO = "BULLISH" | "BEARISH" | "NEUTRAL";

export interface MarketTrendsWindowDTO {
	startsAt: string;
	endsAt: string;
}

export interface TopHotPlayerDTO {
	playerId: number;
	playerName: string;
	boughtTokens: number;
	buyVolume: number;
}

export interface MarketTrendsResponseDTO {
	window: MarketTrendsWindowDTO;
	topHotPlayers: TopHotPlayerDTO[];
	buyTransactionsPercentage: number | null;
	sellTransactionsPercentage: number | null;
	marketBias: MarketBiasDTO | null;
	totalTransactions: number;
	totalTradedTokens: number;
	totalTradedCredits: number;
}
