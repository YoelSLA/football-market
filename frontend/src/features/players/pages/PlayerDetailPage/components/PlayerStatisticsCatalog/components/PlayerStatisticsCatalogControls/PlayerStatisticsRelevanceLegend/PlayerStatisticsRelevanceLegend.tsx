import styles from "./PlayerStatisticsRelevanceLegend.module.scss";

export default function PlayerStatisticsRelevanceLegend() {
	return (
		<div
			className={styles.playerStatisticsRelevanceLegend}
			role="note"
			aria-label="Leyenda de relevancia de métricas"
		>
			<span className={styles.playerStatisticsRelevanceLegend__title}>
				Relevancia:
			</span>

			<span className={styles.playerStatisticsRelevanceLegend__item}>
				<span
					className={`${styles.playerStatisticsRelevanceLegend__dot} ${styles.playerStatisticsRelevanceLegend__dot_principal}`}
					aria-hidden="true"
				/>
				Principal
			</span>

			<span className={styles.playerStatisticsRelevanceLegend__item}>
				<span
					className={`${styles.playerStatisticsRelevanceLegend__dot} ${styles.playerStatisticsRelevanceLegend__dot_secondary}`}
					aria-hidden="true"
				/>
				Secundaria
			</span>

			<span className={styles.playerStatisticsRelevanceLegend__item}>
				<span
					className={`${styles.playerStatisticsRelevanceLegend__dot} ${styles.playerStatisticsRelevanceLegend__dot_low}`}
					aria-hidden="true"
				/>
				Poco relevante
			</span>
		</div>
	);
}
