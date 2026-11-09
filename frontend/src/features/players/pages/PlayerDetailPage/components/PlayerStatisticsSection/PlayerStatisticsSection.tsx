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

function formatDate(dateStr: string | null): string {
	if (!dateStr) return "";
	const parts = dateStr.split("-");
	if (parts.length === 3) {
		const [year, month, day] = parts;
		return `${day}/${month}/${year}`;
	}
	return dateStr;
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
	const availableMetricsCount =
		statistics?.metrics.filter((m) => m.value !== null).length ?? 0;

	return (
		<div className={styles.statisticsSection}>
			<PlayerStatisticsTabs activeTab={activeTab} onChange={onTabChange} />

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
											? ` del ${formatDate(statistics.extractionDate)}`
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

			<div
				id="panel-summary"
				role="tabpanel"
				aria-labelledby="tab-summary"
				hidden={activeTab !== "summary"}
				aria-busy={statisticsState === "loading"}
				className={styles.statisticsSection__tabpanel}
			>
				{statistics ? (
					<>
						{statistics.status === "NO_DATA" && (
							<div
								className={styles.statisticsSection__bannerNotice}
								role="status"
							>
								<strong>
									No hay estadísticas confirmadas para esta temporada.
								</strong>
								<p>
									Las 60 métricas siguen disponibles en el catálogo y muestran
									«Sin datos».
								</p>
							</div>
						)}

						{statistics.status === "PARTIAL" && (
							<div
								className={styles.statisticsSection__bannerNotice}
								role="status"
							>
								<strong>
									Datos parciales: {availableMetricsCount} de 60 métricas
									disponibles.
								</strong>
								<p>Los valores ausentes no se consideran cero.</p>
							</div>
						)}

						<PlayerStatisticsSummary
							statistics={statistics}
							position={player.position}
						/>
					</>
				) : statisticsState === "loading" ? (
					<div
						className={styles.statisticsSection__bannerLoading}
						role="status"
					>
						<p>Consultando estadísticas del jugador…</p>
					</div>
				) : (
					statisticsState === "error" && (
						<div
							className={styles.statisticsSection__bannerErrorPlaceholder}
							role="alert"
						>
							<p>
								{statisticsErrorMessage ||
									"No se pudieron cargar las estadísticas del jugador."}
							</p>
						</div>
					)
				)}
			</div>

			<div
				id="panel-catalog"
				role="tabpanel"
				aria-labelledby="tab-catalog"
				hidden={activeTab !== "catalog"}
				aria-busy={statisticsState === "loading"}
				className={styles.statisticsSection__tabpanel}
			>
				{statistics ? (
					<>
						{statistics.status === "NO_DATA" && (
							<div
								className={styles.statisticsSection__bannerNotice}
								role="status"
							>
								<strong>
									No hay estadísticas confirmadas para esta temporada.
								</strong>
								<p>
									Las 60 métricas siguen disponibles en el catálogo y muestran
									«Sin datos».
								</p>
							</div>
						)}

						{statistics.status === "PARTIAL" && (
							<div
								className={styles.statisticsSection__bannerNotice}
								role="status"
							>
								<strong>
									Datos parciales: {availableMetricsCount} de 60 métricas
									disponibles.
								</strong>
								<p>Los valores ausentes no se consideran cero.</p>
							</div>
						)}

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
					</>
				) : statisticsState === "loading" ? (
					<div
						className={styles.statisticsSection__bannerLoading}
						role="status"
					>
						<p>Consultando estadísticas del jugador…</p>
					</div>
				) : null}
			</div>
		</div>
	);
}
