import axios from "axios";
import { getErrorCode, http } from "@/infrastructure/http";
import {
	toBuyResult,
	toMarketQuote,
	toOrderBuyRequestDTO,
} from "./market.mapper";
import type { BuyResult, MarketQuote, OrderBuyResponseDTO } from "./types";
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

export const marketService = {
	getCurrentQuote,
	buyTokens,
};
