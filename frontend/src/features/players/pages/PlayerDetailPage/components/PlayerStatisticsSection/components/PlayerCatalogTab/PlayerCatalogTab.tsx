import type {
	PlayerStatistics,
	QueryState,
	StatisticsTab,
} from "@/features/players/types";
import PlayerStatisticsCatalog from "../../../PlayerStatisticsCatalog";
import PlayerStatisticsNotice from "../PlayerStatisticsNotice";
import styles from "./PlayerCatalogTab.module.scss";

interface Props {
	activeTab: StatisticsTab;
	statistics: PlayerStatistics | undefined;
	position: string;
	statisticsState: QueryState;
	search: string;
	category: string;
	onSearchChange: (value: string) => void;
	onCategoryChange: (value: string) => void;
	onClearFilters: () => void;
}

export default function PlayerCatalogTab({
	activeTab,
	statistics,
	position,
	statisticsState,
	search,
	category,
	onSearchChange,
	onCategoryChange,
	onClearFilters,
}: Props) {
	return (
		<div
			id="panel-catalog"
			role="tabpanel"
			aria-labelledby="tab-catalog"
			hidden={activeTab !== "catalog"}
			aria-busy={statisticsState === "loading"}
			className={styles.catalogTab}
		>
			{statistics ? (
				<>
					{statistics.status === "NO_DATA" && (
						<PlayerStatisticsNotice
							title="No hay estadísticas confirmadas para esta temporada."
							description="Las 60 métricas siguen disponibles en el catálogo y muestran «Sin datos»."
						/>
					)}

					<PlayerStatisticsCatalog
						metrics={statistics.metrics}
						position={position}
						search={search}
						category={category}
						onSearchChange={onSearchChange}
						onCategoryChange={onCategoryChange}
						onClear={onClearFilters}
					/>
				</>
			) : statisticsState === "loading" ? (
				<div className={styles.catalogTab__loading} role="status">
					<p>Consultando estadísticas del jugador…</p>
				</div>
			) : null}
		</div>
	);
}
