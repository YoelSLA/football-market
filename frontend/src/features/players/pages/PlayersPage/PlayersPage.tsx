import { PageLayout } from "@/app/layouts";
import MarketPagination from "@/features/market/components/MarketPagination";
import { usePlayersPage } from "@/features/players/hooks";
import PlayersCatalog from "./components/PlayersCatalog";
import PlayersHeader from "./components/PlayersHeader";
import PlayersToolbar from "./components/PlayersToolbar";
import styles from "./PlayersPage.module.scss";

export default function PlayersPage() {
	const page = usePlayersPage();
	const players = page.playersPage?.players ?? [];
	const hasActiveFilters = Boolean(
		page.searchText.trim() ||
			page.criteria.name ||
			page.criteria.leagueId ||
			page.criteria.teamId ||
			page.criteria.position ||
			page.criteria.nationality ||
			(page.criteria.sort && page.criteria.sort !== "name,asc"),
	);

	const showPagination =
		Boolean(page.playersPage && players.length) &&
		(page.playersPage?.totalPages ?? 0) > 0;

	return (
		<PageLayout>
			<div className={styles.playersPage} aria-busy={page.isLoading}>
				<div className={styles.playersPage__header}>
					<PlayersHeader
						totalElements={page.playersPage?.totalElements}
						options={page.options}
						isLoading={page.isLoading}
						optionsLoading={page.optionsLoading}
					/>
				</div>

				<div className={styles.playersPage__toolbar}>
					<PlayersToolbar
						searchText={page.searchText}
						onSearchChange={page.setSearchText}
						criteria={page.criteria}
						options={page.options}
						optionsLoading={page.optionsLoading}
						optionsError={page.optionsError}
						onChangeFilter={page.changeFilter}
						onClearFilters={page.clearFilters}
						onRetryOptions={page.retryOptions}
					/>
				</div>

				<div className={styles.playersPage__scrollRegion}>
					<PlayersCatalog
						isLoading={page.isLoading}
						errorMessage={page.errorMessage}
						onRetry={page.retry}
						players={players}
						hasActiveFilters={hasActiveFilters}
					/>
				</div>

				{showPagination && page.playersPage && (
					<div className={styles.playersPage__footer}>
						<MarketPagination
							page={page.selectedPage}
							totalPages={page.playersPage.totalPages}
							visiblePlayers={players.length}
							totalPlayers={page.playersPage.totalElements}
							pageSize={page.playersPage.size}
							showSummary={false}
							variant="official"
							onPageChange={page.selectPage}
						/>
					</div>
				)}
			</div>
		</PageLayout>
	);
}
