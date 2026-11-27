import type {
	PlayerStatistics,
	QueryState,
	StatisticsTab,
} from "@/features/players/types";
import PlayerStatisticsSummary from "../../../PlayerStatisticsSummary";
import PlayerStatisticsNotice from "../PlayerStatisticsNotice";
import styles from "./PlayerSummaryTab.module.scss";

interface Props {
	activeTab: StatisticsTab;
	statistics: PlayerStatistics | undefined;
	position: string;
	statisticsState: QueryState;
	statisticsErrorMessage?: string | null;
}

export default function PlayerSummaryTab({
	activeTab,
	statistics,
	position,
	statisticsState,
	statisticsErrorMessage,
}: Props) {
	return (
		<div
			id="panel-summary"
			role="tabpanel"
			aria-labelledby="tab-summary"
			hidden={activeTab !== "summary"}
			aria-busy={statisticsState === "loading"}
			className={styles.summaryTab}
		>
			{statistics ? (
				<>
					{statistics.status === "NO_DATA" && (
						<PlayerStatisticsNotice
							title="No hay estadísticas confirmadas para esta temporada."
							description="Las 60 métricas siguen disponibles en el catálogo y muestran «Sin datos»."
						/>
					)}

					<PlayerStatisticsSummary
						statistics={statistics}
						position={position}
					/>
				</>
			) : statisticsState === "loading" ? (
				<div className={styles.summaryTab__loading} role="status">
					<p>Consultando estadísticas del jugador…</p>
				</div>
			) : (
				statisticsState === "error" && (
					<div className={styles.summaryTab__errorPlaceholder} role="alert">
						<p>
							{statisticsErrorMessage ||
								"No se pudieron cargar las estadísticas del jugador."}
						</p>
					</div>
				)
			)}
		</div>
	);
}
