import { PageLayout } from "@/app/layouts";
import { usePortfolioHoldingsPage } from "@/features/portfolio/hooks";
import PortfolioHoldingsContent from "./PortfolioHoldingsContent";
import PortfolioHoldingsHeader from "./PortfolioHoldingsHeader";
import styles from "./PortfolioHoldingsPage.module.scss";
import PortfolioHoldingsPagination from "./PortfolioHoldingsPagination";
import PortfolioPositionDetail from "./PortfolioPositionDetail";

export default function PortfolioHoldingsPage() {
	const page = usePortfolioHoldingsPage();
	const data = page.portfolio;

	const totalPages = data?.pagination.totalPages ?? 0;
	const showPagination = totalPages > 0;

	const footer = showPagination ? (
		<PortfolioHoldingsPagination
			page={page.selectedPage}
			totalPages={totalPages}
			totalElements={data?.pagination.totalElements ?? 0}
			pageSize={page.pageSize}
			onPageChange={page.selectPage}
		/>
	) : undefined;

	return (
		<div className={styles.portfolioHoldings} aria-busy={page.isLoading}>
			<PageLayout
				header={
					data && (
						<PortfolioHoldingsHeader
							filter={page.filter}
							onFilterChange={page.selectFilter}
							totalElements={data.pagination.totalElements}
						/>
					)
				}
				footer={footer}
			>
				<PortfolioHoldingsContent
					isLoading={page.isLoading}
					errorMessage={page.errorMessage}
					positions={data?.positions ?? []}
					filter={page.filter}
					onRetry={page.retry}
					onOpenDetail={page.openDetail}
				/>
			</PageLayout>
			{page.selectedPosition && (
				<PortfolioPositionDetail
					key={page.selectedPosition.playerId}
					position={page.selectedPosition}
					onClose={page.closeDetail}
				/>
			)}
		</div>
	);
}
