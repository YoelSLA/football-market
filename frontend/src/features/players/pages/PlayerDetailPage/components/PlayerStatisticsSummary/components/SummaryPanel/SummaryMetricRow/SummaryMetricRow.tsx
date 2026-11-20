import clsx from "clsx";
import type { ResolvedSummaryMetric } from "@/features/players/utils";
import styles from "./SummaryMetricRow.module.scss";

interface Props {
	metric: ResolvedSummaryMetric;
}

export default function SummaryMetricRow({ metric }: Props) {
	const MetricIcon = metric.icon;

	return (
		<li className={styles.summaryMetricRow}>
			<div className={styles.summaryMetricRow__labelGroup}>
				<MetricIcon
					size={24}
					aria-hidden="true"
					className={styles.summaryMetricRow__icon}
				/>

				<span className={styles.summaryMetricRow__label}>{metric.label}</span>
			</div>

			<div className={styles.summaryMetricRow__valueGroup}>
				<strong
					className={clsx(
						styles.summaryMetricRow__value,
						metric.isNull && styles.summaryMetricRow__value_null,
					)}
				>
					{metric.formattedValue}
				</strong>

				{metric.unit && !metric.isNull && (
					<span className={styles.summaryMetricRow__unit}>{metric.unit}</span>
				)}
			</div>
		</li>
	);
}
