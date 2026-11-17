import MarketFilters from "@/features/market/components/MarketFilters";
import MarketPagination from "@/features/market/components/MarketPagination";
import type { MarketBuyState } from "@/features/market/hooks";
import MarketBuyCatalog from "../MarketBuyCatalog";
import styles from "./OfficialMarketPanel.module.scss";

interface Props {
	marketBuy: MarketBuyState;
}

export default function OfficialMarketPanel({ marketBuy }: Props) {
	const hasFilters =
		marketBuy.criteria.name !== "" ||
		marketBuy.criteria.leagueId !== null ||
		marketBuy.criteria.teamId !== null ||
		marketBuy.criteria.position !== null ||
		marketBuy.criteria.nationality !== null ||
		marketBuy.criteria.minPrice !== null ||
		marketBuy.criteria.maxPrice !== null;
	return (
		<>
			<div className={styles.officialPanel__content}>
				<MarketFilters
					variant="official"
					search={marketBuy.searchDraft}
					league={marketBuy.criteria.leagueId?.toString() ?? "all"}
					team={marketBuy.criteria.teamId?.toString() ?? "all"}
					position={marketBuy.criteria.position ?? "all"}
					nationality={marketBuy.criteria.nationality ?? "all"}
					sort={marketBuy.criteria.sort}
					minPrice={marketBuy.minPriceDraft}
					maxPrice={marketBuy.maxPriceDraft}
					rangeError={marketBuy.rangeError}
					isUpdating={marketBuy.isUpdatingPlayers}
					officialOptions={marketBuy.filterOptions}
					onSearchChange={marketBuy.setSearchDraft}
					onLeagueChange={(value) => marketBuy.changeFilter("leagueId", value)}
					onTeamChange={(value) => marketBuy.changeFilter("teamId", value)}
					onPositionChange={(value) =>
						marketBuy.changeFilter("position", value)
					}
					onNationalityChange={(value) =>
						marketBuy.changeFilter("nationality", value)
					}
					onSortChange={(value) => marketBuy.changeFilter("sort", value)}
					onMinPriceChange={marketBuy.setMinPriceDraft}
					onMaxPriceChange={marketBuy.setMaxPriceDraft}
					onClear={marketBuy.clearFilters}
				/>
				<MarketBuyCatalog
					isLoading={marketBuy.isLoadingPlayers}
					isUpdating={marketBuy.isUpdatingPlayers}
					isError={marketBuy.isPlayersError || marketBuy.filterOptionsError}
					errorMessage={marketBuy.playersErrorMessage}
					onRetry={marketBuy.retryPlayers}
					players={marketBuy.players}
					hasFilters={hasFilters}
					onBuyClick={marketBuy.openBuyModal}
				/>
			</div>
			{!marketBuy.isLoadingPlayers && !marketBuy.isPlayersError && (
				<div className={styles.officialPanel__footer}>
					<MarketPagination
						page={marketBuy.criteria.page}
						totalPages={marketBuy.totalPages}
						visiblePlayers={marketBuy.players.length}
						pageSize={marketBuy.pageSize}
						showSummary={false}
						onPageChange={marketBuy.setCurrentPage}
					/>
				</div>
			)}
		</>
	);
}
