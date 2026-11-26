import styles from "./MarketTrendsSkeleton.module.scss";

export default function MarketTrendsSkeleton() {
	return (
		<div
			className={styles.skeletonContainer}
			role="status"
			aria-busy="true"
			aria-label="Cargando análisis de tendencias"
		>
			<div className={styles.topRow}>
				<div className={`${styles.box} ${styles.gaugeBox}`} />
				<div className={`${styles.box} ${styles.sentimentBarBox}`} />
			</div>

			<div className={styles.metricsRow}>
				<div className={`${styles.box} ${styles.metricCard}`} />
				<div className={`${styles.box} ${styles.metricCard}`} />
				<div className={`${styles.box} ${styles.metricCard}`} />
			</div>

			<div className={`${styles.box} ${styles.chartBox}`} />

			<div className={styles.hotAssetsGrid}>
				<div className={`${styles.box} ${styles.hotAssetCard}`} />
				<div className={`${styles.box} ${styles.hotAssetCard}`} />
				<div className={`${styles.box} ${styles.hotAssetCard}`} />
			</div>
		</div>
	);
}
