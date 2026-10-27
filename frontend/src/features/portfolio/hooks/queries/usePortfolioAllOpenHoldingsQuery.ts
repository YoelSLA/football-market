import { useQueries } from "@tanstack/react-query";
import { portfolioQueryKeys } from "@/features/portfolio/constants";
import { portfolioService } from "@/features/portfolio/portfolio.service";
import type { Portfolio } from "@/features/portfolio/types";
import { usePortfolioHoldingsQuery } from "./usePortfolioHoldingsQuery";

const PAGE_SIZE = 6;

export function usePortfolioAllOpenHoldingsQuery() {
	const firstQuery = usePortfolioHoldingsQuery("OPEN", 0, PAGE_SIZE);
	const pageCount = firstQuery.data?.pagination.totalPages ?? 1;
	const laterQueries = useQueries({
		queries: Array.from({ length: Math.max(0, pageCount - 1) }, (_, index) => {
			const page = index + 1;
			return {
				queryKey: portfolioQueryKeys.holdings("OPEN", page, PAGE_SIZE),
				queryFn: ({ signal }: { signal: AbortSignal }) =>
					portfolioService.getPortfolio(
						{ page, size: PAGE_SIZE, status: "OPEN" },
						signal,
					),
			};
		}),
	});
	const firstPage = firstQuery.data;
	const positions = [
		...(firstPage?.positions ?? []),
		...laterQueries.flatMap((query) => query.data?.positions ?? []),
	];
	const isPending = firstQuery.isPending;
	const isFetching =
		firstQuery.isFetching || laterQueries.some((query) => query.isFetching);
	const isError =
		firstQuery.isError || laterQueries.some((query) => query.isError);
	const refetch = async () => {
		await Promise.all([
			firstQuery.refetch(),
			...laterQueries.map((query) => query.refetch()),
		]);
	};
	return {
		data: firstPage ? ({ ...firstPage, positions } as Portfolio) : undefined,
		isPending,
		isFetching,
		isError,
		error: firstQuery.error ?? laterQueries.find((query) => query.error)?.error,
		refetch,
	};
}
