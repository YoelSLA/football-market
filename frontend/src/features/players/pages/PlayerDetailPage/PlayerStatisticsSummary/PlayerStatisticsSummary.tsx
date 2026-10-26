import { Activity, BarChart3, CircleDot, Footprints, Goal, Shield, Star, Clock } from "@/shared/icons";
import type { PlayerStatistics } from "@/features/players/types";
import { getStatisticsProfile, getStatisticsPanels, getStatisticsMetric, getStatisticsPosition, formatStatisticsValue } from "@/features/players/utils";
import styles from "./PlayerStatisticsSummary.module.scss";

interface Props { statistics: PlayerStatistics; position: string }

export default function PlayerStatisticsSummary({ statistics, position }: Props) {
	const profile = getStatisticsProfile(position);
	const positionIndex = getStatisticsPosition(position);
	const panels = getStatisticsPanels(position);
	const cardKeys = profile?.cards ?? ["minutes_played_total", "appearances_total", "whoscored_rating_average"];
	const additional = profile?.additional ?? [];
	const icons = [Goal, CircleDot, Footprints, Star];
	const rating = getStatisticsMetric(statistics, "whoscored_rating_average");
	const renderRows = (keys: string[], bars = false) => {
		const metrics = keys.map((key) => getStatisticsMetric(statistics, key));
		const maximum = Math.max(0, ...metrics.map((item) => item.value ?? 0));
		return <dl className={styles.playerStatisticsSummary__rows}>{metrics.map((item) => <div key={item.key}>
			<dt><CircleDot size={15} aria-hidden="true" />{item.label}</dt>
			{bars && <span className={styles.playerStatisticsSummary__track} aria-hidden="true"><span style={{ width: `${maximum > 0 && item.value !== null ? item.value / maximum * 100 : 0}%` }} /></span>}
			<dd>{formatStatisticsValue(item.value)}{item.unit === "%" && item.value !== null ? " %" : ""}</dd>
		</div>)}</dl>;
	};
	return <section className={styles.playerStatisticsSummary} aria-label="Resumen estadístico por posición">
		{!profile && <p role="status">No hay un perfil estadístico definido para esta posición. Se muestran datos generales.</p>}
		<div className={styles.playerStatisticsSummary__cards}>{cardKeys.map((key, index) => {
			const item = getStatisticsMetric(statistics, key);
			const Icon = icons[index];
			return <article key={key} className={styles.playerStatisticsSummary__card}><span className={styles.playerStatisticsSummary__icon}><Icon size={32} aria-hidden="true" /></span><div><h2>{item.label}</h2><strong>{formatStatisticsValue(item.value)}{item.unit === "%" && item.value !== null ? " %" : ""}</strong><small>{item.unit === "%" ? "Porcentaje" : item.unit}</small></div></article>;
		})}</div>
		<div className={styles.playerStatisticsSummary__grid}>
			{panels.map((panel) => <section key={panel.title} className={styles.playerStatisticsSummary__panel}>
				<h3><Goal size={18} aria-hidden="true" />{panel.title}<small>Por partido</small></h3>
				{"zones" in panel && panel.zones ? <div className={styles.playerStatisticsSummary__pitch}><div className={styles.playerStatisticsSummary__goal} aria-hidden="true" /><div className={styles.playerStatisticsSummary__zones}>{panel.keys.map((key, index) => {
					const item = getStatisticsMetric(statistics, key);
					return <div key={key}><strong>{formatStatisticsValue(item.value)}</strong><span>{["Área chica", "Área penal", "Fuera del área"][index]}</span><small>{item.unit}</small></div>;
				})}</div></div> : renderRows(panel.keys, true)}
				{panel.keys.some((key) => getStatisticsMetric(statistics, key).value === null) && <p className={styles.playerStatisticsSummary__note}>Datos parciales: las métricas ausentes no se representan como cero.</p>}
			</section>)}
			<section className={styles.playerStatisticsSummary__panel}><h3><Activity size={18} aria-hidden="true" />Calificación de la temporada</h3><div className={styles.playerStatisticsSummary__rating}><Star size={38} aria-hidden="true" /><strong>{formatStatisticsValue(rating.value)}</strong><span>WhoScored · promedio de temporada</span></div><p className={styles.playerStatisticsSummary__note}>No hay una serie por partido disponible para representar evolución.</p></section>
			<section className={styles.playerStatisticsSummary__panel}><h3><BarChart3 size={18} aria-hidden="true" />Estadísticas destacadas</h3><p className={styles.playerStatisticsSummary__note}>Valores originales y unidades indicadas; sin puntuación comparativa.</p>{renderRows(additional.slice(0, 7))}</section>
			<section className={styles.playerStatisticsSummary__panel}><h3><Shield size={18} aria-hidden="true" />{["Portería", "Defensa", "Mediocampo", "Ataque"][positionIndex] ?? "Datos generales"}</h3>{renderRows([...cardKeys, ...additional.slice(0, 3)])}</section>
			<section className={styles.playerStatisticsSummary__panel}><h3><Clock size={18} aria-hidden="true" />Participación y distribución</h3>{renderRows(["minutes_played_total", "appearances_total", "passes_per_match", "pass_completion_percentage", "successful_long_balls_per_match", "yellow_cards_total", "red_cards_total"])}</section>
		</div>
	</section>;
}
