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
	listings: TokenListingDTO[];
}

export interface OrderSellRequestDTO {
	playerId: number;
	quantity: number;
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
