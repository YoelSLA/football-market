import axios from "axios";
import { OFFICIAL_MARKET_DEFAULT_PAGE_SIZE } from "@/features/market/constants";
import { getErrorCode, http } from "@/infrastructure/http";
import {
	toBuyResult,
	toCancelListingResult,
	toMarketListings,
	toMarketQuote,
	toOfficialMarketPage,
	toOrderBuyRequestDTO,
	toOrderSellOfficialRequestDTO,
	toOrderSellRequestDTO,
	toP2PMarketPage,
	toSellOfficialResult,
	toSellResult,
	toUserOrdersPage,
} from "./market.mapper";
import type {
	BuyResult,
	CancelListingResult,
	CommunityMarketCriteria,
	MarketListing,
	MarketQuote,
	MyOrdersQueryParams,
	OfficialMarketCriteria,
	OfficialMarketPage,
	OrderBuyResponseDTO,
	OrderSellOfficialResponseDTO,
	OrderSellResponseDTO,
	P2PMarketPage,
	SellOfficialResult,
	SellResult,
	UserOrdersPage,
} from "./types";
import { MarketQuoteReadError } from "./types";

/**
 * Servicio HTTP para Market (SPEC-019).
 * Realiza solicitudes autenticadas a cotización vigente y compra de tokens.
 */

async function getCurrentQuote(
	playerId: number,
	signal?: AbortSignal,
): Promise<MarketQuote | null> {
	try {
		const { data } = await http.get<unknown>(
			`/players/${playerId}/quotes/current`,
			{
				authenticated: true,
				signal,
			},
		);
		return toMarketQuote(data);
	} catch (error: unknown) {
		if (axios.isCancel(error) || error instanceof MarketQuoteReadError) {
			throw error;
		}
		if (
			axios.isAxiosError(error) &&
			error.response?.status === 404 &&
			getErrorCode(error) === "QUOTE_NOT_FOUND"
		) {
			return null;
		}
		const status = axios.isAxiosError(error)
			? error.response?.status
			: undefined;
		if (status === 401)
			throw new MarketQuoteReadError("authentication-error", status);
		if (status === 403) throw new MarketQuoteReadError("forbidden", status);
		throw new MarketQuoteReadError(
			status ? "http-error" : "network-error",
			status,
		);
	}
}

async function buyTokens(request: {
	playerId: number;
	quantity: number;
}): Promise<BuyResult> {
	const dto = toOrderBuyRequestDTO(request);
	const { data } = await http.post<OrderBuyResponseDTO>("/orders/buy", dto, {
		authenticated: true,
	});
	return toBuyResult(data);
}

async function sellTokens(request: {
	playerId: number;
	quantity: number;
	unitPrice?: number;
}): Promise<SellResult> {
	const dto = toOrderSellRequestDTO(request);
	const { data } = await http.post<OrderSellResponseDTO>("/orders/sell", dto, {
		authenticated: true,
	});
	return toSellResult(data);
}

async function sellTokensOfficial(request: {
	playerId: number;
	quantity: number;
}): Promise<SellOfficialResult> {
	const dto = toOrderSellOfficialRequestDTO(request);
	const { data } = await http.post<OrderSellOfficialResponseDTO>(
		"/orders/sell-official",
		dto,
		{
			authenticated: true,
		},
	);
	return toSellOfficialResult(data);
}

async function getListings(
	playerId?: number,
	signal?: AbortSignal,
): Promise<MarketListing[]> {
	const { data } = await http.get<unknown>("/listings", {
		authenticated: true,
		signal,
		params: playerId ? { playerId } : undefined,
	});
	return toMarketListings(data);
}

async function getP2PMarket(
	criteria: CommunityMarketCriteria,
	signal?: AbortSignal,
): Promise<P2PMarketPage> {
	const { data } = await http.get<unknown>("/market/p2p", {
		authenticated: true,
		signal,
		params: {
			page: criteria.page,
			size: 8,
			sort: criteria.sort,
			search: criteria.search || undefined,
			leagueId: criteria.leagueId ?? undefined,
			teamId: criteria.teamId ?? undefined,
			position: criteria.position ?? undefined,
			minPrice: criteria.minPrice ?? undefined,
			maxPrice: criteria.maxPrice ?? undefined,
		},
	});
	return toP2PMarketPage(data);
}

async function getOfficialMarket(
	criteria: OfficialMarketCriteria,
	signal?: AbortSignal,
): Promise<OfficialMarketPage> {
	const { data } = await http.get<unknown>("/market/official", {
		authenticated: true,
		signal,
		params: {
			page: criteria.page,
			size: OFFICIAL_MARKET_DEFAULT_PAGE_SIZE,
			sort: criteria.sort,
			hasQuote: true,
			name: criteria.name || undefined,
			leagueId: criteria.leagueId ?? undefined,
			teamId: criteria.teamId ?? undefined,
			position: criteria.position ?? undefined,
			nationality: criteria.nationality ?? undefined,
			minPrice: criteria.minPrice ?? undefined,
			maxPrice: criteria.maxPrice ?? undefined,
		},
	});
	return toOfficialMarketPage(data);
}

async function buyListing(listingId: number): Promise<BuyResult> {
	const { data } = await http.post<OrderBuyResponseDTO>(
		"/orders/buy",
		{ listingId },
		{ authenticated: true },
	);
	const result = toBuyResult(data);
	if (result.listingId !== listingId)
		throw new Error("INVALID_BUY_LISTING_RESPONSE");
	return result;
}

async function getMyListings(
	params: MyOrdersQueryParams,
	signal?: AbortSignal,
): Promise<UserOrdersPage> {
	const { data } = await http.get<unknown>("/listings/mine", {
		authenticated: true,
		signal,
		params: {
			page: params.page,
			size: params.size ?? 10,
			status: params.status || undefined,
			sort: params.sort || "NEWEST",
		},
	});
	return toUserOrdersPage(data);
}

async function cancelListing(listingId: number): Promise<CancelListingResult> {
	const { data } = await http.post<unknown>(
		`/listings/${listingId}/cancel`,
		{},
		{ authenticated: true },
	);
	return toCancelListingResult(data);
}

export const marketService = {
	getCurrentQuote,
	buyTokens,
	sellTokens,
	sellTokensOfficial,
	getListings,
	getP2PMarket,
	getOfficialMarket,
	buyListing,
	getMyListings,
	cancelListing,
};
