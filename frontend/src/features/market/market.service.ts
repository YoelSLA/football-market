import axios from "axios";
import { OFFICIAL_MARKET_DEFAULT_PAGE_SIZE } from "@/features/market/constants";
import { getErrorCode, http } from "@/infrastructure/http";
import {
	toBuyResult,
	toMarketListings,
	toMarketQuote,
	toOfficialMarketPage,
	toOrderBuyRequestDTO,
	toOrderSellRequestDTO,
	toP2PMarketPage,
	toSellResult,
} from "./market.mapper";
import type {
	BuyResult,
	CommunityMarketCriteria,
	MarketListing,
	MarketQuote,
	OfficialMarketCriteria,
	OfficialMarketPage,
	OrderBuyResponseDTO,
	OrderSellResponseDTO,
	P2PMarketPage,
	SellResult,
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
}): Promise<SellResult> {
	const dto = toOrderSellRequestDTO(request);
	const { data } = await http.post<OrderSellResponseDTO>("/orders/sell", dto, {
		authenticated: true,
	});
	return toSellResult(data);
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

export const marketService = {
	getCurrentQuote,
	buyTokens,
	sellTokens,
	getListings,
	getP2PMarket,
	getOfficialMarket,
	buyListing,
};
