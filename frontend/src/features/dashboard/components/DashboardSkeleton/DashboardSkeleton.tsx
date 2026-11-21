import styles from "./DashboardSkeleton.module.scss";

export interface DashboardSkeletonProps {
	className?: string;
}

export function DashboardSkeleton({ className }: DashboardSkeletonProps) {
	return (
		<section
			className={`${styles.skeletonContainer} ${className ?? ""}`.trim()}
			aria-busy="true"
			aria-label="Cargando panel de control del mercado..."
		>
			<div className={styles.scrollRegion}>
				{/* Header skeleton */}
				<div className={styles.headerSkeleton}>
					<div className={styles.headerTitleGroup}>
						<div className={styles.headerIconSkeleton} />
						<div className={styles.headerTextSkeleton}>
							<div className={styles.headerTitleSkeleton} />
							<div className={styles.headerDescSkeleton} />
						</div>
					</div>
					<div className={styles.headerMetricsSkeleton}>
						<div className={styles.metricItemSkeleton} />
						<div className={styles.metricItemSkeleton} />
					</div>
				</div>

				{/* Ticker bar skeleton */}
				<div className={styles.tickerSkeleton} />

				{/* Cockpit 4 cards skeleton */}
				<div className={styles.cockpitGrid}>
					<div className={styles.cardSkeleton} />
					<div className={styles.cardSkeleton} />
					<div className={styles.cardSkeleton} />
					<div className={styles.cardSkeleton} />
				</div>

				{/* Quick Actions Bar skeleton */}
				<div className={styles.quickActionsGrid}>
					<div className={styles.quickActionSkeleton} />
					<div className={styles.quickActionSkeleton} />
					<div className={styles.quickActionSkeleton} />
					<div className={styles.quickActionSkeleton} />
				</div>

				{/* Middle charts grid skeleton */}
				<div className={styles.chartsGrid}>
					<div className={styles.donutSkeleton} />
					<div className={styles.benchmarkSkeleton} />
				</div>

				{/* Showcase cards skeleton */}
				<div className={styles.showcaseSkeleton}>
					<div className={styles.showcaseHeaderSkeleton} />
					<div className={styles.showcaseCardsGrid}>
						<div className={styles.showcaseCardSkeleton} />
						<div className={styles.showcaseCardSkeleton} />
						<div className={styles.showcaseCardSkeleton} />
						<div className={styles.showcaseCardSkeleton} />
					</div>
				</div>

				{/* Market Row skeleton */}
				<div className={styles.marketRowSkeleton}>
					<div className={styles.bottomCardSkeleton} />
					<div className={styles.bottomCardSkeleton} />
				</div>
			</div>
		</section>
	);
}
