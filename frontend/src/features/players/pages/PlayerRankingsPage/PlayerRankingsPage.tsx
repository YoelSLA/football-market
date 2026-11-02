import { usePlayerRankingsPage } from "@/features/players/hooks";
import RankingContent from "./components/RankingContent";
import RankingsStatus from "./components/RankingStatus";
import PlayerRankingsFilters from "./components/RankingsFilters";
import PlayerRankingsHeader from "./components/RankingsHeader";
import PlayerRankingsPagination from "./components/RankingsPagination";
import PlayerRankingsPodium from "./components/RankingsPodium";
import PlayerRankingsTable from "./components/RankingsTable";

export default function PlayerRankingsPage() {
	const {
		mode,
		handleModeChange,
		metadata,
		podiumPlayers,
		tableItems,
		allRankedItems,
		filteredCount,
		totalCount,
		filters,
		handleFilterChange,
		handleClearFilters,
		availableLeagues,
		pagination,
		status,
	} = usePlayerRankingsPage();

	const hasActiveFilters =
		Boolean(filters.searchTerm.trim()) ||
		Boolean(filters.selectedLeague) ||
		Boolean(filters.selectedPosition);

	return (
		<RankingContent isLoading={status.isLoading}>
			<PlayerRankingsHeader
				mode={mode}
				onModeChange={handleModeChange}
				metadata={metadata}
			/>

			<RankingsStatus
				status={status}
				mode={mode}
				isEmpty={allRankedItems.length === 0}
				isFilterEmpty={filteredCount === 0}
				hasActiveFilters={hasActiveFilters}
				onClearFilters={handleClearFilters}
			>
				<PlayerRankingsPodium topPlayers={podiumPlayers} mode={mode} />

				<PlayerRankingsFilters
					filters={filters}
					onFilterChange={handleFilterChange}
					onClearFilters={handleClearFilters}
					availableLeagues={availableLeagues}
					totalCount={totalCount}
					filteredCount={filteredCount}
				/>

				{filteredCount > 0 && (
					<>
						<PlayerRankingsTable items={tableItems} mode={mode} />

						<PlayerRankingsPagination
							currentPage={pagination.currentPage}
							totalPages={pagination.totalPages}
							totalItems={pagination.totalItems}
							onPageChange={pagination.onPageChange}
						/>
					</>
				)}
			</RankingsStatus>
		</RankingContent>
	);
}
