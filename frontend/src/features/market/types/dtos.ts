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
