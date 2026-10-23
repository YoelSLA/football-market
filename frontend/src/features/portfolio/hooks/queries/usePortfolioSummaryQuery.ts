import { useQuery } from "@tanstack/react-query";
import { portfolioQueryKeys } from "@/features/portfolio/constants";
import { portfolioService } from "@/features/portfolio/portfolio.service";

export function usePortfolioSummaryQuery() {
	return useQuery({
		queryKey: portfolioQueryKeys.summary,
		queryFn: ({ signal }) =>
			portfolioService.getPortfolio({ page: 0, size: 1 }, signal),
	});
}
