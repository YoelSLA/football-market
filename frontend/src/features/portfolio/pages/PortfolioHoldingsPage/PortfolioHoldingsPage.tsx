import { PageLayout } from "@/app/layouts";
import { usePortfolioHoldingsPage } from "@/features/portfolio/hooks";
import PortfolioHoldingsContent from "./components/PortfolioHoldingsContent";
import PortfolioHoldingsHeader from "./components/PortfolioHoldingsHeader";
import PortfolioHoldingsPagination from "./components/PortfolioHoldingsPagination";
import PortfolioPositionDetail from "./components/PortfolioPositionDetail";
import styles from "./PortfolioHoldingsPage.module.scss";

export default function PortfolioHoldingsPage() {
	const page = usePortfolioHoldingsPage();
	const data = page.portfolio;

	const totalPages = data?.pagination.totalPages ?? 0;
	const showPagination = totalPages > 0;

	const pagination = showPagination ? (
		<PortfolioHoldingsPagination
			page={page.selectedPage}
			totalPages={totalPages}
			totalElements={data?.pagination.totalElements ?? 0}
			pageSize={page.pageSize}
			onPageChange={page.selectPage}
		/>
	) : undefined;

	return (
		<PageLayout>
			<div className={styles.portfolioHoldings} aria-busy={page.isLoading}>
				{data && (
					<div className={styles.portfolioHoldings__header}>
						<PortfolioHoldingsHeader
							filter={page.filter}
							onFilterChange={page.selectFilter}
							totalElements={data.pagination.totalElements}
						/>
					</div>
				)}

				<div className={styles.portfolioHoldings__scrollRegion}>
					<PortfolioHoldingsContent
						isLoading={page.isLoading}
						errorMessage={page.errorMessage}
						positions={data?.positions ?? []}
						filter={page.filter}
						onRetry={page.retry}
						onOpenDetail={page.openDetail}
					/>
				</div>

				{pagination && (
					<div className={styles.portfolioHoldings__pagination}>
						{pagination}
					</div>
				)}

				{page.selectedPosition && (
					<PortfolioPositionDetail
						key={page.selectedPosition.playerId}
						position={page.selectedPosition}
						onClose={page.closeDetail}
					/>
				)}
			</div>
		</PageLayout>
	);
}
