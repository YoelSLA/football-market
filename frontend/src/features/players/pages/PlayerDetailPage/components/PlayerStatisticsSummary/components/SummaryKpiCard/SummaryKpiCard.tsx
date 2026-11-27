import clsx from "clsx";
import type { ResolvedSummaryMetric } from "@/features/players/utils";
import styles from "./SummaryKpiCard.module.scss";

interface Props {
	metric: ResolvedSummaryMetric;
}

export default function SummaryKpiCard({ metric }: Props) {
	const Icon = metric.icon;

	return (
		<article
			className={styles.summaryKpiCard}
			aria-label={`${metric.label}: ${metric.formattedValue}${metric.unit === "%" ? " %" : ""}`}
		>
			<span className={styles.summaryKpiCard__icon} aria-hidden="true">
				<Icon size={24} />
			</span>
			<div className={styles.summaryKpiCard__content}>
				<h3 className={styles.summaryKpiCard__title}>{metric.label}</h3>
				<div className={styles.summaryKpiCard__valueWrapper}>
					<strong
						className={clsx(
							styles.summaryKpiCard__value,
							metric.isNull && styles.summaryKpiCard__value_null,
						)}
					>
						{metric.formattedValue}
					</strong>
					{metric.unit === "%" && !metric.isNull && (
						<span className={styles.summaryKpiCard__unit}>%</span>
					)}
				</div>
				<small className={styles.summaryKpiCard__subtitle}>
					{metric.subtitle}
				</small>
			</div>
		</article>
	);
}
