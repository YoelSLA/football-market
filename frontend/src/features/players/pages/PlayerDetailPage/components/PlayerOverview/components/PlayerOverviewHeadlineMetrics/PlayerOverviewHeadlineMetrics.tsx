import {
	getHeadlineMetricLabel,
	getHeadlineMetricUnit,
	HEADLINE_METRIC_KEYS,
} from "@/features/players/constants";
import type { PlayerStatistics } from "@/features/players/types";
import {
	formatStatisticsValue,
	getMetricIcon,
	getStatisticsMetric,
	getStatisticsPosition,
	getStatisticsPositionLabel,
} from "@/features/players/utils";
import styles from "./PlayerOverviewHeadlineMetrics.module.scss";

interface Props {
	statistics: PlayerStatistics | undefined;
	position: string;
	className?: string;
}

export default function PlayerOverviewHeadlineMetrics({
	statistics,
	position,
	className,
}: Props) {
	const positionLabel = getStatisticsPositionLabel(position);
	const positionIndex = getStatisticsPosition(position);
	const headlineKeys = HEADLINE_METRIC_KEYS[positionIndex] ?? [];

	return (
		<section
			className={`${styles.headlineMetrics} ${className ?? ""}`.trim()}
			aria-label={`Estadísticas principales de ${positionLabel.toLowerCase()}`}
		>
			{headlineKeys.map((key) => {
				const metric = statistics
					? getStatisticsMetric(statistics, key)
					: { key, label: key, unit: "", value: null };
				const isPercentage = metric.unit === "%";
				const Icon = getMetricIcon(key);

				return (
					<div className={styles.headlineMetrics__card} key={metric.key}>
						<Icon
							size={18}
							className={styles.headlineMetrics__icon}
							aria-hidden="true"
						/>
						<span className={styles.headlineMetrics__label}>
							{statistics ? metric.label : getHeadlineMetricLabel(key)}
						</span>
						<strong className={styles.headlineMetrics__value}>
							{formatStatisticsValue(metric.value)}
							{isPercentage && metric.value !== null ? " %" : ""}
						</strong>
						<small className={styles.headlineMetrics__unit}>
							{getHeadlineMetricUnit(key)}
						</small>
					</div>
				);
			})}
		</section>
	);
}
