import { useQuery } from "@tanstack/react-query";
import { portfolioQueryKeys } from "@/features/portfolio/constants";
import { portfolioService } from "@/features/portfolio/portfolio.service";
import type { TransactionHistoryQueryParams } from "@/features/portfolio/types";

export function useTransactionHistoryQuery(
	params: TransactionHistoryQueryParams,
) {
	return useQuery({
		queryKey: portfolioQueryKeys.transactions(params),
		queryFn: ({ signal }) =>
			portfolioService.getTransactionHistory(params, signal),
		placeholderData: (previousData) => previousData,
	});
}
