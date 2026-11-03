import type { PlayerDetail, PlayerStatistics } from "@/features/players/types";
import { Button } from "@/shared/components";
import PlayerStatisticsCatalog from "../PlayerStatisticsCatalog";
import PlayerStatisticsSummary from "../PlayerStatisticsSummary";
import PlayerStatisticsTabs from "../PlayerStatisticsTabs";
import styles from "./PlayerStatisticsSection.module.scss";

interface Props {
	player: PlayerDetail;
	statistics: PlayerStatistics | undefined;
	statisticsState: string;
	statisticsErrorMessage?: string | null;
	activeTab: "summary" | "catalog";
	onTabChange: (tab: "summary" | "catalog") => void;
	onRetryStatistics: () => void;
	search: string;
	category: string;
	unit: string;
	onSearchChange: (value: string) => void;
	onCategoryChange: (value: string) => void;
	onUnitChange: (value: string) => void;
	onClearFilters: () => void;
}

export default function PlayerStatisticsSection({
	player,
	statistics,
	statisticsState,
	statisticsErrorMessage,
	activeTab,
	onTabChange,
	onRetryStatistics,
	search,
	category,
	unit,
	onSearchChange,
	onCategoryChange,
	onUnitChange,
	onClearFilters,
}: Props) {
	return (
		<div className={styles.statisticsSection}>
			<PlayerStatisticsTabs activeTab={activeTab} onChange={onTabChange} />

			{statisticsState === "error" && (
				<section role="alert" className={styles.statisticsSection__message}>
					<p>{statisticsErrorMessage}</p>
					{statistics && (
						<p>
							Mostrando la última consulta disponible; no se pudo actualizar
						</p>
					)}
					<Button type="button" onClick={onRetryStatistics}>
						Reintentar
					</Button>
				</section>
			)}

			<div
				id="panel-summary"
				role="tabpanel"
				aria-labelledby="tab-summary"
				hidden={activeTab !== "summary"}
				aria-busy={statisticsState === "loading"}
			>
				{statistics ? (
					<>
						<p className={styles.statisticsSection__context}>
							{statistics.status === "NO_DATA"
								? "No hay estadísticas disponibles para este jugador y temporada."
								: statistics.status === "PARTIAL"
									? "Algunas estadísticas no están disponibles para esta temporada."
									: "Estadísticas de la temporada"}
						</p>
						<PlayerStatisticsSummary
							statistics={statistics}
							position={player.position}
						/>
					</>
				) : (
					statisticsState === "loading" && (
						<p role="status">Cargando estadísticas…</p>
					)
				)}
			</div>

			<div
				id="panel-catalog"
				role="tabpanel"
				aria-labelledby="tab-catalog"
				hidden={activeTab !== "catalog"}
				aria-busy={statisticsState === "loading"}
			>
				{statistics ? (
					<PlayerStatisticsCatalog
						metrics={statistics.metrics}
						position={player.position}
						search={search}
						category={category}
						unit={unit}
						onSearchChange={onSearchChange}
						onCategoryChange={onCategoryChange}
						onUnitChange={onUnitChange}
						onClear={onClearFilters}
					/>
				) : (
					statisticsState === "loading" && (
						<p role="status">Cargando estadísticas…</p>
					)
				)}
			</div>
		</div>
	);
}
