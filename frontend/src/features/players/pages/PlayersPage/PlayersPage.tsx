import { PageLayout } from "@/app/layouts";
import { usePlayersPage } from "@/features/players/hooks";
import { Pagination } from "@/shared/components";
import PlayersCatalog from "./components/PlayersCatalog";
import PlayersToolbar from "./components/PlayersToolbar";
import styles from "./PlayersPage.module.scss";

export default function PlayersPage() {
	const page = usePlayersPage();
	const players = page.playersPage?.players ?? [];
	const hasActiveFilters = Boolean(
		page.criteria.name ||
			page.criteria.leagueId ||
			page.criteria.teamId ||
			page.criteria.position ||
			page.criteria.nationality,
	);

	const showPagination =
		Boolean(page.playersPage && players.length) &&
		(page.playersPage?.totalPages ?? 0) > 0;

	return (
		<PageLayout>
			<div
				className={`${styles.playersPage} ${showPagination ? styles.playersPage_withFloatingPagination : ""}`}
				aria-busy={page.isLoading}
			>
				<PlayersToolbar
					totalElements={page.playersPage?.totalElements}
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
					<div className={styles.playersPage__pagination}>
						<Pagination
							page={page.selectedPage}
							totalPages={page.playersPage.totalPages}
							onChange={page.selectPage}
						/>
					</div>
				)}
			</div>
		</PageLayout>
	);
}
