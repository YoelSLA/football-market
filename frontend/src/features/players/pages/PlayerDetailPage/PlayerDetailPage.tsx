import { useParams } from "react-router-dom";
import { PageLayout } from "@/app/layouts";
import { usePlayerDetailPage } from "@/features/players/hooks";
import PlayerDetailNav from "./components/PlayerDetailNav";
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
				<PlayerDetailNav />

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
							statistics={detail.statistics}
							quote={detail.currentQuote}
						/>

						<PlayerStatisticsSection
							player={detail.player}
							statistics={detail.statistics}
							statisticsState={detail.statisticsState}
							statisticsErrorMessage={detail.statisticsErrorMessage}
							quotes={detail.quotes}
							quotesState={detail.quotesState}
							quotesErrorMessage={detail.quotesErrorMessage}
							onRetryQuotes={detail.retryQuotes}
							activeTab={detail.activeTab}
							onTabChange={detail.setActiveTab}
							onRetryStatistics={detail.retryStatistics}
							search={detail.search}
							category={detail.category}
							onSearchChange={detail.setSearch}
							onCategoryChange={detail.setCategory}
							onClearFilters={detail.clearFilters}
						/>
					</div>
				)}
			</div>
		</PageLayout>
	);
}
