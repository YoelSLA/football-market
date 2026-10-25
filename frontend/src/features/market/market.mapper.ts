import type {
	BuyResult,
	MarketQuote,
	OrderBuyRequestDTO,
	OrderBuyResponseDTO,
	QuoteResponseDTO,
} from "./types";
import { MarketQuoteReadError } from "./types";

/**
 * Mapper para transformar DTOs a Models y viceversa dentro de la frontera de Service.
 */

function isQuoteResponseDTO(value: unknown): value is QuoteResponseDTO {
	if (typeof value !== "object" || value === null) return false;
	return (
		"price" in value &&
		typeof value.price === "number" &&
		Number.isSafeInteger(value.price) &&
		value.price > 0 &&
		"effectiveDate" in value &&
		typeof value.effectiveDate === "string" &&
		/^\d{4}-\d{2}-\d{2}$/.test(value.effectiveDate) &&
		Number.isFinite(Date.parse(value.effectiveDate)) &&
		new Date(value.effectiveDate).toISOString().slice(0, 10) === value.effectiveDate &&
		"calculatedAt" in value &&
		typeof value.calculatedAt === "string" &&
		/^\d{4}-\d{2}-\d{2}T.+(?:Z|[+-]\d{2}:\d{2})$/.test(value.calculatedAt) &&
		Number.isFinite(Date.parse(value.calculatedAt))
	);
}

export function toMarketQuote(dto: unknown): MarketQuote {
	if (!isQuoteResponseDTO(dto)) {
		throw new MarketQuoteReadError("invalid-response");
	}
	return {
		price: dto.price,
		effectiveDate: dto.effectiveDate,
		calculatedAt: dto.calculatedAt,
	};
}

export function toBuyResult(dto: OrderBuyResponseDTO): BuyResult {
	return {
		transactionId: dto.transactionId,
		playerId: dto.playerId,
		quantity: dto.quantity,
		unitPrice: dto.unitPrice,
		totalAmount: dto.totalAmount,
		executedAt: dto.executedAt,
		listingId: dto.listingId,
	};
}

export function toOrderBuyRequestDTO(params: {
	playerId: number;
	quantity: number;
}): OrderBuyRequestDTO {
	return {
		playerId: params.playerId,
		quantity: params.quantity,
	};
}
