import { PageLayout } from "@/app/layouts";
import MarketPagination from "@/features/market/components/MarketPagination";
import { usePlayerRankingsPage } from "@/features/players/hooks";
import RankingsStatus from "./components/RankingStatus";
import PlayerRankingsFilters from "./components/RankingsFilters";
import PlayerRankingsHeader from "./components/RankingsHeader";
import PlayerRankingsPodium from "./components/RankingsPodium";
import PlayerRankingsTable from "./components/RankingsTable";
import styles from "./PlayerRankingsPage.module.scss";

export default function PlayerRankingsPage() {
	const {
		mode,
		metadata,
		podiumItems,
		tableItems,
		filters,
		handleFilterChange,
		handleClearFilters,
		filterOptions,
		hasActiveFilters,
		pagination,
		status,
	} = usePlayerRankingsPage();

	return (
		<PageLayout>
			<main
				className={styles.playerRankings}
				aria-busy={status.isLoading}
				aria-label="Página de ranking de jugadores"
			>
				<PlayerRankingsHeader
					metadata={metadata}
					isLoading={status.isLoading}
				/>

				<div className={styles.playerRankings__scrollRegion}>
					<RankingsStatus
						status={status}
						mode={mode}
						isGlobalEmpty={status.isGlobalEmpty}
						isFilterEmpty={status.isFilterEmpty}
						hasActiveFilters={hasActiveFilters}
						onClearFilters={handleClearFilters}
					>
						{podiumItems.length > 0 && (
							<PlayerRankingsPodium topPlayers={podiumItems} mode={mode} />
						)}

						<PlayerRankingsTable
							items={tableItems}
							toolbar={
								<PlayerRankingsFilters
									filters={filters}
									onFilterChange={handleFilterChange}
									onClearFilters={handleClearFilters}
									availableLeagues={filterOptions.leagues}
									availablePositions={filterOptions.positions}
								/>
							}
						/>
					</RankingsStatus>
				</div>

				{!status.isLoading && !status.isError && pagination.totalItems > 0 && (
					<footer className={styles.playerRankings__footer}>
						<MarketPagination
							page={pagination.currentPage - 1}
							totalPages={pagination.totalPages}
							visiblePlayers={pagination.itemsOnPage}
							totalPlayers={pagination.totalItems}
							pageSize={pagination.pageSize}
							showSummary={false}
							variant="official"
							onPageChange={(zeroBasedPage) =>
								pagination.onPageChange(zeroBasedPage + 1)
							}
						/>
					</footer>
				)}
			</main>
		</PageLayout>
	);
}
