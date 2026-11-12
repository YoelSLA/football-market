import MarketPagination from "@/features/market/components/MarketPagination";
import type { CommunityMarketState } from "@/features/market/hooks";
import CommunityListingsPanel from "./CommunityListingsPanel";
import CommunityMarket from "./CommunityMarket";
import CommunityMarketFilters from "./CommunityMarketFilters";
import styles from "./CommunityMarketPanel.module.scss";

interface Props {
	community: CommunityMarketState;
}

export default function CommunityMarketPanel({ community }: Props) {
	if (community.selectedPlayer) {
		return (
			<div className={styles.communityPanel__content}>
				<CommunityListingsPanel
					player={community.selectedPlayer}
					onBack={community.closePlayer}
					onBuy={community.openListing}
				/>
			</div>
		);
	}

	return (
		<>
			<div className={styles.communityPanel__content}>
				<CommunityMarketFilters
					filters={community.filters}
					leagues={community.leagues}
					positions={community.positions}
					onChange={community.updateFilters}
					onClear={community.clearFilters}
				/>
				<CommunityMarket
					isLoading={community.isLoading}
					isError={community.isError}
					errorMessage={community.errorMessage}
					players={community.players}
					onRetry={community.retry}
					onOpenPlayer={community.openPlayer}
				/>
			</div>
			{community.totalPages > 1 && (
				<div className={styles.communityPanel__footer}>
					<MarketPagination
						page={community.page}
						totalPages={community.totalPages}
						totalPlayers={community.totalPlayers}
						visiblePlayers={community.players.length}
						pageSize={12}
						onPageChange={community.setPage}
					/>
				</div>
			)}
		</>
	);
}
