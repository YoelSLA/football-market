import type {
	BuyResult,
	MarketQuote,
	OrderBuyRequestDTO,
	OrderBuyResponseDTO,
	OrderSellRequestDTO,
	OrderSellResponseDTO,
	QuoteResponseDTO,
	SellResult,
} from "./types";
import { MarketQuoteReadError } from "./types";

/**
 * Mapper para transformar DTOs a Models y viceversa dentro de la frontera de Service.
 */

function isQuoteResponseDTO(value: unknown): value is QuoteResponseDTO {
	if (typeof value !== "object" || value === null) return false;
	const candidate = value as Record<string, unknown>;
	return (
		"price" in candidate &&
		typeof candidate.price === "number" &&
		Number.isSafeInteger(candidate.price) &&
		candidate.price > 0 &&
		"effectiveDate" in candidate &&
		typeof candidate.effectiveDate === "string" &&
		/^\d{4}-\d{2}-\d{2}$/.test(candidate.effectiveDate) &&
		Number.isFinite(Date.parse(candidate.effectiveDate)) &&
		new Date(candidate.effectiveDate).toISOString().slice(0, 10) ===
			candidate.effectiveDate &&
		"calculatedAt" in candidate &&
		typeof candidate.calculatedAt === "string" &&
		/^\d{4}-\d{2}-\d{2}T.+(?:Z|[+-]\d{2}:\d{2})$/.test(
			candidate.calculatedAt,
		) &&
		Number.isFinite(Date.parse(candidate.calculatedAt)) &&
		(!("previousPrice" in candidate) ||
			candidate.previousPrice === null ||
			candidate.previousPrice === undefined ||
			(typeof candidate.previousPrice === "number" &&
				Number.isSafeInteger(candidate.previousPrice) &&
				candidate.previousPrice >= 0))
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
		previousPrice:
			typeof dto.previousPrice === "number" ? dto.previousPrice : null,
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

export function toSellResult(dto: OrderSellResponseDTO): SellResult {
	return {
		listingId: dto.listingId,
		playerId: dto.playerId,
		quantity: dto.quantity,
		unitPrice: dto.unitPrice,
		totalAmount: dto.totalAmount,
		status: dto.status,
		createdAt: dto.createdAt,
		expiresAt: dto.expiresAt,
	};
}

export function toOrderSellRequestDTO(params: {
	playerId: number;
	quantity: number;
}): OrderSellRequestDTO {
	return { playerId: params.playerId, quantity: params.quantity };
}
