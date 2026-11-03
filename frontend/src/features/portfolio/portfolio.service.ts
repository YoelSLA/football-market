import { http } from "@/infrastructure/http";
import { toPortfolio } from "./portfolio.mapper";
import type {
	Portfolio,
	PortfolioQueryParams,
	PortfolioResponseDTO,
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

export const portfolioService = { getPortfolio };
