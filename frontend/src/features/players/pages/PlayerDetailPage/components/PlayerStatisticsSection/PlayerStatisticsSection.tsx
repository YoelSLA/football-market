import type {
	PlayerDetail,
	PlayerQuoteHistory,
	PlayerStatistics,
	QueryState,
	StatisticsTab,
} from "@/features/players/types";
import { formatDate } from "@/features/players/utils";
import { Button } from "@/shared/components";
import PlayerStatisticsTabs from "../PlayerStatisticsTabs";
import PlayerCatalogTab from "./components/PlayerCatalogTab";
import PlayerQuotesTab from "./components/PlayerQuotesTab";
import PlayerSummaryTab from "./components/PlayerSummaryTab";
import styles from "./PlayerStatisticsSection.module.scss";

interface Props {
	player: PlayerDetail;
	statistics: PlayerStatistics | undefined;
	statisticsState: QueryState;
	statisticsErrorMessage?: string | null;
	quotes?: PlayerQuoteHistory;
	quotesState?: QueryState;
	quotesErrorMessage?: string | null;
	onRetryQuotes?: () => void;
	activeTab: StatisticsTab;
	onTabChange: (tab: StatisticsTab) => void;
	onRetryStatistics: () => void;
	search: string;
	category: string;
	onSearchChange: (value: string) => void;
	onCategoryChange: (value: string) => void;
	onClearFilters: () => void;
}

export default function PlayerStatisticsSection({
	player,
	statistics,
	statisticsState,
	statisticsErrorMessage,
	quotes: _quotes,
	quotesState = "ready",
	quotesErrorMessage,
	onRetryQuotes,
	activeTab,
	onTabChange,
	onRetryStatistics,
	search,
	category,
	onSearchChange,
	onCategoryChange,
	onClearFilters,
}: Props) {
	return (
		<div className={styles.statisticsSection}>
			<PlayerStatisticsTabs activeTab={activeTab} onChange={onTabChange} />

			<PlayerQuotesTab
				activeTab={activeTab}
				quotesState={quotesState}
				quotesErrorMessage={quotesErrorMessage}
				onRetryQuotes={onRetryQuotes}
			/>

			{statisticsState === "error" && (
				<section role="alert" className={styles.statisticsSection__bannerError}>
					<div className={styles.statisticsSection__bannerText}>
						<strong>
							{statistics
								? "No se pudo actualizar."
								: "No se pudieron consultar las estadísticas."}
						</strong>
						<p>
							{statistics
								? `Mostrando la última consulta disponible${
										statistics.extractionDate
											? ` del ${formatDate(statistics.extractionDate, "")}`
											: ""
									}.`
								: "La identidad y la navegación permanecen disponibles."}
						</p>
					</div>
					<Button type="button" onClick={onRetryStatistics}>
						Reintentar
					</Button>
				</section>
			)}

			<PlayerSummaryTab
				activeTab={activeTab}
				statistics={statistics}
				position={player.position}
				statisticsState={statisticsState}
				statisticsErrorMessage={statisticsErrorMessage}
			/>

			<PlayerCatalogTab
				activeTab={activeTab}
				statistics={statistics}
				position={player.position}
				statisticsState={statisticsState}
				search={search}
				category={category}
				onSearchChange={onSearchChange}
				onCategoryChange={onCategoryChange}
				onClearFilters={onClearFilters}
			/>
		</div>
	);
}
