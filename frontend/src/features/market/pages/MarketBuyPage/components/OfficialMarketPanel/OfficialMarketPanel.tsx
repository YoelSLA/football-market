import MarketFilters from "@/features/market/components/MarketFilters";
import MarketPagination from "@/features/market/components/MarketPagination";
import type { MarketBuyState } from "@/features/market/hooks";
import MarketBuyCatalog from "../MarketBuyCatalog";
import styles from "./OfficialMarketPanel.module.scss";

interface Props {
	marketBuy: MarketBuyState;
}

export default function OfficialMarketPanel({ marketBuy }: Props) {
	return (
		<>
			<div className={styles.officialPanel__content}>
				<MarketFilters />
				<MarketBuyCatalog
					isLoading={marketBuy.isLoadingPlayers}
					isError={marketBuy.isPlayersError}
					errorMessage={marketBuy.playersErrorMessage}
					onRetry={marketBuy.retryPlayers}
					players={marketBuy.players}
					getQuoteState={marketBuy.quotesQuery.getQuoteState}
					onBuyClick={marketBuy.openBuyModal}
				/>
			</div>
			<div className={styles.officialPanel__footer}>
				<MarketPagination
					page={marketBuy.currentPage}
					totalPages={marketBuy.totalPages}
					totalPlayers={marketBuy.totalPlayers}
					visiblePlayers={marketBuy.players.length}
					pageSize={marketBuy.pageSize}
					onPageChange={marketBuy.setCurrentPage}
				/>
			</div>
		</>
	);
}
