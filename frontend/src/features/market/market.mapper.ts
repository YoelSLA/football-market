import type { Player } from "@/features/players";
import type {
	BuyResult,
	MarketListing,
	MarketPlayer,
	MarketQuote,
	OfficialMarketPage,
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

function isSafeNonNegativeInteger(value: unknown): value is number {
	return typeof value === "number" && Number.isSafeInteger(value) && value >= 0;
}

function isFiniteNumber(value: unknown): value is number {
	return typeof value === "number" && Number.isFinite(value);
}

function isNullableString(value: unknown): value is string | null {
	return value === null || typeof value === "string";
}

function isNullablePositiveInteger(value: unknown): value is number | null {
	return value === null || isSafePositiveInteger(value);
}

function isIsoDate(value: unknown): value is string {
	return (
		typeof value === "string" &&
		/^\d{4}-\d{2}-\d{2}$/.test(value) &&
		Number.isFinite(Date.parse(value)) &&
		new Date(value).toISOString().slice(0, 10) === value
	);
}

function isIsoInstant(value: unknown): value is string {
	return (
		typeof value === "string" &&
		/^\d{4}-\d{2}-\d{2}T.+(?:Z|[+-]\d{2}:\d{2})$/.test(value) &&
		Number.isFinite(Date.parse(value))
	);
}

function toOfficialQuote(value: unknown) {
	if (typeof value !== "object" || value === null)
		throw new Error("INVALID_OFFICIAL_MARKET_QUOTE");
	const quote = value as Record<string, unknown>;
	if (
		!isSafePositiveInteger(quote.price) ||
		!(
			quote.previousPrice === null ||
			isSafeNonNegativeInteger(quote.previousPrice)
		) ||
		!(
			quote.variationPercentage === null ||
			isFiniteNumber(quote.variationPercentage)
		) ||
		!isFiniteNumber(quote.performanceScore) ||
		!isFiniteNumber(quote.multiplier) ||
		typeof quote.calculationOrigin !== "string" ||
		quote.calculationOrigin.length === 0 ||
		!isIsoDate(quote.effectiveDate) ||
		!isIsoInstant(quote.calculatedAt)
	)
		throw new Error("INVALID_OFFICIAL_MARKET_QUOTE");
	return {
		price: quote.price,
		previousPrice: quote.previousPrice,
		variationPercentage: quote.variationPercentage,
		effectiveDate: quote.effectiveDate,
	};
}

export function toOfficialMarketPage(value: unknown): OfficialMarketPage {
	if (typeof value !== "object" || value === null)
		throw new Error("INVALID_OFFICIAL_MARKET_RESPONSE");
	const envelope = value as Record<string, unknown>;
	if (
		!Array.isArray(envelope.content) ||
		!isSafeNonNegativeInteger(envelope.page) ||
		!isSafePositiveInteger(envelope.size) ||
		envelope.size > 100 ||
		!isSafeNonNegativeInteger(envelope.totalElements) ||
		!isSafeNonNegativeInteger(envelope.totalPages) ||
		envelope.content.length > envelope.size ||
		envelope.totalPages !== Math.ceil(envelope.totalElements / envelope.size)
	)
		throw new Error("INVALID_OFFICIAL_MARKET_RESPONSE");

	const players = envelope.content.map((value) => {
		if (typeof value !== "object" || value === null)
			throw new Error("INVALID_OFFICIAL_MARKET_PLAYER");
		const player = value as Record<string, unknown>;
		if (
			!isSafePositiveInteger(player.id) ||
			typeof player.name !== "string" ||
			player.name.trim().length === 0 ||
			typeof player.position !== "string" ||
			player.position.trim().length === 0 ||
			!isNullableString(player.nationality) ||
			!isNullablePositiveInteger(player.teamId) ||
			!isNullableString(player.teamName) ||
			!isNullableString(player.teamBadgeUrl) ||
			!isNullablePositiveInteger(player.leagueId) ||
			!isNullableString(player.leagueName) ||
			!isNullableString(player.imageUrl) ||
			!(
				player.quote === null ||
				(typeof player.quote === "object" && player.quote !== null)
			)
		)
			throw new Error("INVALID_OFFICIAL_MARKET_PLAYER");
		return {
			id: player.id,
			name: player.name,
			position: player.position,
			nationality: player.nationality,
			teamId: player.teamId,
			teamName: player.teamName,
			teamBadgeUrl: player.teamBadgeUrl,
			leagueId: player.leagueId,
			leagueName: player.leagueName,
			imageUrl: player.imageUrl,
			fallbackImageUrl: null,
			quote: player.quote === null ? null : toOfficialQuote(player.quote),
		};
	});

	return {
		players,
		page: envelope.page,
		size: envelope.size,
		totalElements: envelope.totalElements,
		totalPages: envelope.totalPages,
	};
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
