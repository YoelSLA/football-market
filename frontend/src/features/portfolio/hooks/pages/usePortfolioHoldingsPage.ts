import { useEffect, useState } from "react";
import { portfolioLabels } from "@/features/portfolio/constants";
import { usePortfolioHoldingsQuery } from "@/features/portfolio/hooks/queries";
import type {
	PortfolioPosition,
	PositionFilter,
} from "@/features/portfolio/types";
import { getErrorMessage } from "@/infrastructure/http";

export function usePortfolioHoldingsPage() {
	const [page, setPage] = useState(0);
	const pageSize = 6;
	const [filter, setFilter] = useState<PositionFilter>("ALL");
	const [selectedPlayerId, setSelectedPlayerId] = useState<number | null>(null);

	const query = usePortfolioHoldingsQuery(filter, page, pageSize);
	const isLoading = query.isPending || query.isFetching;
	const errorMessage = query.isError
		? getErrorMessage(query.error, portfolioLabels.error)
		: null;
	const portfolio = !isLoading && !errorMessage ? query.data : undefined;

	useEffect(() => {
		if (!query.data) return;
		const lastPage = Math.max(0, query.data.pagination.totalPages - 1);
		if (page > lastPage) setPage(lastPage);
	}, [query.data, page]);

	useEffect(() => {
		if (query.isError) setSelectedPlayerId(null);
	}, [query.isError]);

	// La posición sigue perteneciendo a la caché remota, no se copia a estado cliente.
	const selectedPosition = !errorMessage
		? (query.data?.positions.find(
				(position) => position.playerId === selectedPlayerId,
			) ?? null)
		: null;

	function selectFilter(next: PositionFilter) {
		setFilter(next);
		setPage(0);
	}

	function selectPage(next: number) {
		if (next >= 0 && next < (portfolio?.pagination.totalPages ?? 0))
			setPage(next);
	}

	return {
		portfolio,
		selectedPage: page,
		pageSize,
		filter,
		selectFilter,
		selectPage,
		selectedPosition,
		openDetail: (position: PortfolioPosition) =>
			setSelectedPlayerId(position.playerId),
		closeDetail: () => setSelectedPlayerId(null),
		isLoading,
		errorMessage,
		retry: query.refetch,
	};
}
