import { useParams } from "react-router-dom";
import { PageLayout } from "@/app/layouts";
import { usePlayerDetailPage } from "@/features/players/hooks";
import PlayerBreadcrumb from "./components/PlayerBreadcrumb";
import PlayerDetailStatus from "./components/PlayerDetailStatus";
import PlayerOverview from "./components/PlayerOverview";
import PlayerStatisticsSection from "./components/PlayerStatisticsSection";
import styles from "./PlayerDetailPage.module.scss";

export default function PlayerDetailPage() {
	const { id } = useParams();
	const detail = usePlayerDetailPage(id);

	return (
		<PageLayout>
			<div className={styles.playerDetail}>
				<PlayerBreadcrumb playerName={detail.player?.name} />

				{!detail.player ? (
					<div className={styles.playerDetail__status}>
						<PlayerDetailStatus
							state={detail.detailState}
							errorMessage={detail.errorMessage}
							onRetry={detail.retry}
						/>
					</div>
				) : (
					<div className={styles.playerDetail__container}>
						<PlayerOverview
							player={detail.player}
							seasonLabel={
								detail.statistics?.seasonLabel ??
								detail.statistics?.seasonKey ??
								null
							}
							extractionDate={detail.statistics?.extractionDate ?? null}
							statistics={detail.statistics}
						/>

						<PlayerStatisticsSection
							player={detail.player}
							statistics={detail.statistics}
							statisticsState={detail.statisticsState}
							statisticsErrorMessage={detail.statisticsErrorMessage}
							activeTab={detail.activeTab}
							onTabChange={detail.setActiveTab}
							onRetryStatistics={detail.retryStatistics}
							search={detail.search}
							category={detail.category}
							unit={detail.unit}
							onSearchChange={detail.setSearch}
							onCategoryChange={detail.setCategory}
							onUnitChange={detail.setUnit}
							onClearFilters={detail.clearFilters}
						/>
					</div>
				)}
			</div>
		</PageLayout>
	);
}
