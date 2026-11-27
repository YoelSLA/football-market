import { useNavigate } from "react-router-dom";
import { PageLayout } from "@/app/layouts";
import MarketPagination from "@/features/market/components/MarketPagination";
import { usePortfolioHoldingsPage } from "@/features/portfolio/hooks";
import type { PortfolioPosition } from "@/features/portfolio/types";
import PortfolioCockpit from "./components/PortfolioCockpit";
import PortfolioHoldingsContent from "./components/PortfolioHoldingsContent";
import PortfolioPositionDetail from "./components/PortfolioPositionDetail";
import PortfolioToolbar from "./components/PortfolioToolbar";
import styles from "./PortfolioHoldingsPage.module.scss";

export default function PortfolioHoldingsPage() {
	const navigate = useNavigate();
	const page = usePortfolioHoldingsPage();
	const data = page.portfolio;

	const totalPages = data?.pagination.totalPages ?? 0;
	const showPagination = totalPages > 0;

	const handleSell = (position: PortfolioPosition) => {
		const searchParam = position.playerName
			? `?search=${encodeURIComponent(position.playerName)}`
			: "";
		navigate(`/market/sell${searchParam}`);
	};

	return (
		<PageLayout>
			<div className={styles.portfolioHoldings} aria-busy={page.isLoading}>
				{data?.summary && (
					<PortfolioCockpit summary={data.summary} isLoading={page.isLoading} />
				)}

				{data && (
					<PortfolioToolbar
						filter={page.filter}
						onFilterChange={page.selectFilter}
						totalElements={data.pagination.totalElements}
					/>
				)}

				<div className={styles.portfolioHoldings__scrollRegion}>
					<PortfolioHoldingsContent
						isLoading={page.isLoading}
						errorMessage={page.errorMessage}
						positions={data?.positions ?? []}
						filter={page.filter}
						onRetry={page.retry}
						onOpenDetail={page.openDetail}
						onSell={handleSell}
					/>
				</div>

				{showPagination && data && (
					<div className={styles.portfolioHoldings__pagination}>
						<MarketPagination
							page={page.selectedPage}
							totalPages={totalPages}
							visiblePlayers={data.positions.length}
							totalPlayers={data.pagination.totalElements}
							pageSize={page.pageSize}
							showSummary={false}
							variant="official"
							onPageChange={page.selectPage}
						/>
					</div>
				)}

				{page.selectedPosition && (
					<PortfolioPositionDetail
						key={page.selectedPosition.playerId}
						position={page.selectedPosition}
						onClose={page.closeDetail}
						onSell={handleSell}
					/>
				)}
			</div>
		</PageLayout>
	);
}
