import { useQuery } from "@tanstack/react-query";
import { portfolioQueryKeys } from "@/features/portfolio/constants";
import { portfolioService } from "@/features/portfolio/portfolio.service";
import type { PositionFilter } from "@/features/portfolio/types";

export function usePortfolioHoldingsQuery(
	status: PositionFilter,
	page: number,
	pageSize: number,
) {
	return useQuery({
		queryKey: portfolioQueryKeys.holdings(status, page, pageSize),
		queryFn: ({ signal }) =>
			portfolioService.getPortfolio(
				{
					page,
					size: pageSize,
					...(status === "ALL" ? {} : { status }),
				},
				signal,
			),
	});
}
