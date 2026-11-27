import { http } from "@/infrastructure/http";
import { toPortfolio, toTransactionHistory } from "./portfolio.mapper";
import type {
	OperationHistoryResponseDTO,
	Portfolio,
	PortfolioQueryParams,
	PortfolioResponseDTO,
	TransactionHistory,
	TransactionHistoryQueryParams,
} from "./types";

async function getPortfolio(
	params: PortfolioQueryParams,
	signal: AbortSignal,
): Promise<Portfolio> {
	const { data } = await http.get<PortfolioResponseDTO>("/portfolio/me", {
		authenticated: true,
		params,
		signal,
	});
	return toPortfolio(data);
}

async function getTransactionHistory(
	params: TransactionHistoryQueryParams,
	signal: AbortSignal,
): Promise<TransactionHistory> {
	const { data } = await http.get<OperationHistoryResponseDTO>(
		"/users/me/transactions",
		{
			authenticated: true,
			params,
			signal,
		},
	);
	return toTransactionHistory(data);
}

export const portfolioService = { getPortfolio, getTransactionHistory };
