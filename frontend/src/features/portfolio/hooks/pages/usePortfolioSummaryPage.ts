import { portfolioLabels } from "@/features/portfolio/constants";
import { usePortfolioSummaryQuery } from "@/features/portfolio/hooks/queries";
import { getErrorMessage } from "@/infrastructure/http";

export function usePortfolioSummaryPage() {
	const query = usePortfolioSummaryQuery();
	const isLoading = query.isPending || query.isFetching;
	const errorMessage = query.isError
		? getErrorMessage(query.error, portfolioLabels.error)
		: null;
	return {
		summary: !isLoading && !errorMessage ? query.data?.summary : undefined,
		isLoading,
		errorMessage,
		retry: query.refetch,
		empty: query.data?.pagination.totalElements === 0,
	};
}
