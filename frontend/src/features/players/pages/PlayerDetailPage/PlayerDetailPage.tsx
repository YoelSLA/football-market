import { Link, useParams } from "react-router-dom";
import { PageLayout } from "@/app/layouts";
import { usePlayerDetailPage } from "@/features/players/hooks";
import PlayerBreadcrumb from "./PlayerBreadcrumb";
import PlayerOverview from "./PlayerOverview";
import PlayerStatisticsTabs from "./PlayerStatisticsTabs";
import PlayerStatisticsSummary from "./PlayerStatisticsSummary";
import PlayerStatisticsCatalog from "./PlayerStatisticsCatalog";
import styles from "./PlayerDetailPage.module.scss";

export default function PlayerDetailPage() {
	const { id } = useParams();
	const detail = usePlayerDetailPage(id);
	const statistics = detail.statistics;
	return <div className={styles.playerDetail}><PageLayout header={<PlayerBreadcrumb playerName={detail.player?.name} />}>
		{!detail.player ? <section className={styles.playerDetail__message} role={detail.detailState === "loading" ? "status" : "alert"}><p>{detail.detailState === "loading" ? "Cargando jugador…" : detail.detailState === "invalid-id" ? "El identificador del jugador no es válido." : detail.detailState === "not-found" ? "Jugador no encontrado" : detail.errorMessage}</p>{detail.detailState === "error" && <button type="button" onClick={detail.retry}>Reintentar</button>}<Link to="/players">Volver a jugadores</Link></section> : <div className={styles.playerDetail__content}>
			<PlayerOverview player={detail.player} seasonLabel={statistics?.seasonLabel ?? statistics?.seasonKey ?? null} extractionDate={statistics?.extractionDate ?? null} competitionName={statistics?.competitionName ?? null} />
			<PlayerStatisticsTabs activeTab={detail.activeTab} onChange={detail.setActiveTab} />
			{detail.statisticsState === "error" && <section role="alert" className={styles.playerDetail__message}><p>{detail.statisticsErrorMessage}</p>{statistics && <p>Mostrando la última consulta disponible; no se pudo actualizar</p>}<button type="button" onClick={detail.retryStatistics}>Reintentar</button></section>}
			<div id="panel-summary" role="tabpanel" aria-labelledby="tab-summary" hidden={detail.activeTab !== "summary"} tabIndex={0} aria-busy={detail.statisticsState === "loading"}>
				{statistics ? <><p className={styles.playerDetail__context}>{statistics.status === "NO_DATA" ? "No hay estadísticas disponibles para este jugador y temporada." : statistics.status === "PARTIAL" ? "Algunas estadísticas no están disponibles para esta temporada." : "Estadísticas de la temporada"}</p><PlayerStatisticsSummary statistics={statistics} position={detail.player.position} /></> : detail.statisticsState === "loading" && <p role="status">Cargando estadísticas…</p>}
			</div>
			<div id="panel-catalog" role="tabpanel" aria-labelledby="tab-catalog" hidden={detail.activeTab !== "catalog"} tabIndex={0} aria-busy={detail.statisticsState === "loading"}>
				{statistics ? <PlayerStatisticsCatalog metrics={statistics.metrics} position={detail.player.position} search={detail.search} category={detail.category} unit={detail.unit} onSearchChange={detail.setSearch} onCategoryChange={detail.setCategory} onUnitChange={detail.setUnit} onClear={detail.clearFilters} /> : detail.statisticsState === "loading" && <p role="status">Cargando estadísticas…</p>}
			</div>
		</div>}
	</PageLayout></div>;
}
