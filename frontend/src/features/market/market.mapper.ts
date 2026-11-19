import type { Player } from "@/features/players";
import type {
	BuyResult,
	CancelListingResult,
	CommunityMarketPlayer,
	MarketListing,
	MarketPlayer,
	MarketQuote,
	OfficialMarketPage,
	OrderBuyRequestDTO,
	OrderBuyResponseDTO,
	OrderSellRequestDTO,
	OrderSellResponseDTO,
	P2PMarketPage,
	QuoteResponseDTO,
	SellResult,
	UserOrderListing,
	UserOrdersPage,
	UserOrdersSummary,
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
	const rawPriceRange =
		typeof envelope.priceRange === "object" && envelope.priceRange !== null
			? (envelope.priceRange as Record<string, unknown>)
			: { min: null, max: null };
	if (
		!(
			(rawPriceRange.min === null && rawPriceRange.max === null) ||
			(isSafePositiveInteger(rawPriceRange.min) &&
				isSafePositiveInteger(rawPriceRange.max) &&
				rawPriceRange.min <= rawPriceRange.max)
		)
	)
		throw new Error("INVALID_OFFICIAL_MARKET_RESPONSE");

	const priceRange = {
		min: (rawPriceRange.min as number | null) ?? null,
		max: (rawPriceRange.max as number | null) ?? null,
	};

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
		priceRange: {
			min: priceRange.min as number | null,
			max: priceRange.max as number | null,
		},
	};
}

export function toP2PMarketPage(value: unknown): P2PMarketPage {
	if (typeof value !== "object" || value === null)
		throw new Error("INVALID_P2P_MARKET_RESPONSE");
	const envelope = value as Record<string, unknown>;
	if (
		!Array.isArray(envelope.content) ||
		!isSafeNonNegativeInteger(envelope.page) ||
		!isSafePositiveInteger(envelope.size) ||
		envelope.size > 100 ||
		!isSafeNonNegativeInteger(envelope.totalElements) ||
		!isSafeNonNegativeInteger(envelope.totalPages) ||
		typeof envelope.priceRange !== "object" ||
		envelope.priceRange === null ||
		envelope.content.length > envelope.size ||
		envelope.totalPages !== Math.ceil(envelope.totalElements / envelope.size)
	)
		throw new Error("INVALID_P2P_MARKET_RESPONSE");
	const priceRange = envelope.priceRange as Record<string, unknown>;

	const players: CommunityMarketPlayer[] = envelope.content.map((value) => {
		if (typeof value !== "object" || value === null)
			throw new Error("INVALID_P2P_MARKET_PLAYER");
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
			!isSafeNonNegativeInteger(player.listingCount) ||
			!isSafeNonNegativeInteger(player.availableQuantity) ||
			!isSafeNonNegativeInteger(player.minimumUnitPrice)
		)
			throw new Error("INVALID_P2P_MARKET_PLAYER");
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
			listingCount: player.listingCount,
			availableQuantity: player.availableQuantity,
			minimumUnitPrice: player.minimumUnitPrice,
		};
	});

	return {
		players,
		page: envelope.page,
		size: envelope.size,
		totalElements: envelope.totalElements,
		totalPages: envelope.totalPages,
		priceRange: {
			min: (priceRange.min as number | null) ?? null,
			max: (priceRange.max as number | null) ?? null,
		},
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
		teamId: player.teamId,
		teamName: player.teamName,
		leagueId: player.leagueId,
		leagueName: player.leagueName,
		position: player.position,
		nationality: player.nationality,
		imageUrl: player.imageUrl,
		fallbackImageUrl: player.fallbackImageUrl,
	};
}

export function toUserOrderListing(dto: unknown): UserOrderListing {
	if (typeof dto !== "object" || dto === null) {
		throw new Error("INVALID_USER_ORDER_LISTING");
	}
	const candidate = dto as Record<string, unknown>;
	const status = candidate.status;
	if (
		status !== "ACTIVE" &&
		status !== "SOLD" &&
		status !== "CANCELLED" &&
		status !== "EXPIRED"
	) {
		throw new Error("INVALID_LISTING_STATUS");
	}
	return {
		listingId: Number(candidate.listingId),
		sellerId: Number(candidate.sellerId),
		playerId: Number(candidate.playerId),
		quantity: Number(candidate.quantity),
		unitPrice: Number(candidate.unitPrice) / 100,
		totalAmount: Number(candidate.totalAmount) / 100,
		status,
		createdAt: String(candidate.createdAt),
		expiresAt: String(candidate.expiresAt),
	};
}

export function toUserOrdersSummary(dto: unknown): UserOrdersSummary {
	if (typeof dto !== "object" || dto === null) {
		return {
			activeOrders: 0,
			activeTokens: 0,
			activeValue: 0,
			soldOrders: 0,
			soldTokens: 0,
			soldValue: 0,
			cancelledOrders: 0,
			expiredOrders: 0,
			totalOrders: 0,
		};
	}
	const candidate = dto as Record<string, unknown>;
	return {
		activeOrders: Number(candidate.activeOrders) || 0,
		activeTokens: Number(candidate.activeTokens) || 0,
		activeValue: (Number(candidate.activeValue) || 0) / 100,
		soldOrders: Number(candidate.soldOrders) || 0,
		soldTokens: Number(candidate.soldTokens) || 0,
		soldValue: (Number(candidate.soldValue) || 0) / 100,
		cancelledOrders: Number(candidate.cancelledOrders) || 0,
		expiredOrders: Number(candidate.expiredOrders) || 0,
		totalOrders: Number(candidate.totalOrders) || 0,
	};
}

export function toUserOrdersPage(data: unknown): UserOrdersPage {
	if (typeof data !== "object" || data === null) {
		throw new Error("INVALID_USER_ORDERS_PAGE_RESPONSE");
	}
	const candidate = data as Record<string, unknown>;
	const rawListings = Array.isArray(candidate.listings)
		? candidate.listings
		: [];
	const listings = rawListings.map(toUserOrderListing);
	let summary = toUserOrdersSummary(candidate.summary);

	if (!candidate.summary && listings.length > 0) {
		let activeOrders = 0;
		let activeTokens = 0;
		let activeValue = 0;
		let soldOrders = 0;
		let soldTokens = 0;
		let soldValue = 0;
		let cancelledOrders = 0;
		let expiredOrders = 0;

		for (const item of listings) {
			switch (item.status) {
				case "ACTIVE":
					activeOrders++;
					activeTokens += item.quantity;
					activeValue += item.totalAmount;
					break;
				case "SOLD":
					soldOrders++;
					soldTokens += item.quantity;
					soldValue += item.totalAmount;
					break;
				case "CANCELLED":
					cancelledOrders++;
					break;
				case "EXPIRED":
					expiredOrders++;
					break;
			}
		}

		summary = {
			activeOrders,
			activeTokens,
			activeValue,
			soldOrders,
			soldTokens,
			soldValue,
			cancelledOrders,
			expiredOrders,
			totalOrders: listings.length,
		};
	}
	const paginationRecord =
		typeof candidate.pagination === "object" && candidate.pagination !== null
			? (candidate.pagination as Record<string, unknown>)
			: null;
	const page = paginationRecord ? Number(paginationRecord.page) || 0 : 0;
	const size = paginationRecord ? Number(paginationRecord.size) || 10 : 10;
	const totalElements = paginationRecord
		? Number(paginationRecord.totalElements) || listings.length
		: listings.length;
	const totalPages = paginationRecord
		? Number(paginationRecord.totalPages) || (totalElements > 0 ? 1 : 0)
		: totalElements > 0
			? 1
			: 0;

	return {
		summary,
		listings,
		page,
		size,
		totalElements,
		totalPages,
	};
}

export function toCancelListingResult(dto: unknown): CancelListingResult {
	if (typeof dto !== "object" || dto === null) {
		throw new Error("INVALID_CANCEL_LISTING_RESPONSE");
	}
	const candidate = dto as Record<string, unknown>;
	return {
		listingId: Number(candidate.listingId),
		sellerId: Number(candidate.sellerId),
		playerId: Number(candidate.playerId),
		quantity: Number(candidate.quantity),
		unitPrice: Number(candidate.unitPrice) / 100,
		totalAmount: Number(candidate.totalAmount) / 100,
		status: "CANCELLED",
		createdAt: String(candidate.createdAt),
		expiresAt: String(candidate.expiresAt),
		closedAt: candidate.closedAt ? String(candidate.closedAt) : null,
	};
}
