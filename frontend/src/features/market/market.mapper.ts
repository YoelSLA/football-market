import type { Player } from "@/features/players";
import type {
	BuyResult,
	MarketListing,
	MarketPlayer,
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
	if (
		!isSafePositiveInteger(dto.transactionId) ||
		!isSafePositiveInteger(dto.playerId) ||
		!isSafePositiveInteger(dto.quantity) ||
		!isSafePositiveInteger(dto.unitPrice) ||
		!isSafePositiveInteger(dto.totalAmount) ||
		dto.totalAmount !== dto.quantity * dto.unitPrice ||
		!isIsoInstant(dto.executedAt) ||
		(dto.listingId !== undefined && !isSafePositiveInteger(dto.listingId))
	) {
		throw new Error("INVALID_BUY_RESPONSE");
	}
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

function isSafePositiveInteger(value: unknown): value is number {
	return typeof value === "number" && Number.isSafeInteger(value) && value > 0;
}

function isIsoInstant(value: unknown): value is string {
	return (
		typeof value === "string" &&
		/^\d{4}-\d{2}-\d{2}T.+(?:Z|[+-]\d{2}:\d{2})$/.test(value) &&
		Number.isFinite(Date.parse(value))
	);
}

export function toMarketListings(value: unknown): MarketListing[] {
	if (typeof value !== "object" || value === null)
		throw new Error("INVALID_LISTINGS_RESPONSE");
	const envelope = value as Record<string, unknown>;
	if (!Array.isArray(envelope.listings))
		throw new Error("INVALID_LISTINGS_RESPONSE");
	return envelope.listings.map((item) => {
		if (typeof item !== "object" || item === null)
			throw new Error("INVALID_LISTING_RESPONSE");
		const dto = item as Record<string, unknown>;
		if (
			!isSafePositiveInteger(dto.listingId) ||
			!isSafePositiveInteger(dto.sellerId) ||
			!isSafePositiveInteger(dto.playerId) ||
			!isSafePositiveInteger(dto.quantity) ||
			!isSafePositiveInteger(dto.unitPrice) ||
			!isSafePositiveInteger(dto.totalAmount) ||
			dto.totalAmount !== dto.quantity * dto.unitPrice ||
			dto.status !== "ACTIVE" ||
			!isIsoInstant(dto.createdAt) ||
			!isIsoInstant(dto.expiresAt)
		)
			throw new Error("INVALID_LISTING_RESPONSE");
		return {
			listingId: dto.listingId,
			sellerId: dto.sellerId,
			playerId: dto.playerId,
			quantity: dto.quantity,
			unitPrice: dto.unitPrice,
			totalAmount: dto.totalAmount,
			createdAt: dto.createdAt,
			expiresAt: dto.expiresAt,
		};
	});
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

export function toMarketPlayer(player: Player): MarketPlayer {
	return {
		id: player.id,
		name: player.name,
		teamName: player.teamName,
		leagueName: player.leagueName,
		position: player.position,
		imageUrl: player.imageUrl,
		fallbackImageUrl: player.fallbackImageUrl,
	};
}
