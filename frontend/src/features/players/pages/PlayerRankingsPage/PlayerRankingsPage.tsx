import { PageLayout } from "@/app/layouts";
import { usePlayerRankingsPage } from "@/features/players/hooks";
import RankingsStatus from "./components/RankingStatus";
import PlayerRankingsFilters from "./components/RankingsFilters";
import PlayerRankingsHeader from "./components/RankingsHeader";
import PlayerRankingsPagination from "./components/RankingsPagination";
import PlayerRankingsPodium from "./components/RankingsPodium";
import PlayerRankingsTable from "./components/RankingsTable";
import styles from "./PlayerRankingsPage.module.scss";

export default function PlayerRankingsPage() {
	const {
		mode,
		handleModeChange,
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
					mode={mode}
					onModeChange={handleModeChange}
					metadata={metadata}
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

						<PlayerRankingsFilters
							filters={filters}
							onFilterChange={handleFilterChange}
							onClearFilters={handleClearFilters}
							availableLeagues={filterOptions.leagues}
							availablePositions={filterOptions.positions}
						/>

						{tableItems.length > 0 && (
							<PlayerRankingsTable items={tableItems} mode={mode} />
						)}
					</RankingsStatus>
				</div>

				{!status.isLoading && !status.isError && pagination.totalItems > 0 ? (
					<PlayerRankingsPagination
						currentPage={pagination.currentPage}
						totalPages={pagination.totalPages}
						totalItems={pagination.totalItems}
						itemsOnPage={pagination.itemsOnPage}
						pageSize={pagination.pageSize}
						pageSizeOptions={pagination.pageSizeOptions}
						onPageChange={pagination.onPageChange}
						onPageSizeChange={pagination.onPageSizeChange}
					/>
				) : null}
			</main>
		</PageLayout>
	);
}
