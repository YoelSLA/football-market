import type { ReactNode } from "react";
import type { LucideIcon } from "@/shared/icons";
import styles from "./MarketPageHeader.module.scss";

export interface MarketHeaderMetricItem {
	label: string;
	value: ReactNode;
	detail: ReactNode;
	tooltip?: string;
	icon: LucideIcon;
	variant: "blue" | "violet" | "indigo" | "rose";
}

const VARIANT_CLASSES = {
	blue: styles.marketPageHeader__metric_blue,
	violet: styles.marketPageHeader__metric_violet,
	indigo: styles.marketPageHeader__metric_indigo,
	rose: styles.marketPageHeader__metric_rose,
} as const;

export interface MarketPageHeaderProps {
	title: string;
	description: string;
	icon: LucideIcon;
	metrics?: MarketHeaderMetricItem[];
	children?: ReactNode;
}

export default function MarketPageHeader({
	title,
	description,
	icon: Icon,
	metrics,
	children,
}: MarketPageHeaderProps) {
	return (
		<header className={styles.marketPageHeader}>
			<div className={styles.marketPageHeader__left}>
				<div className={styles.marketPageHeader__iconWrapper}>
					<Icon aria-hidden="true" className={styles.marketPageHeader__icon} />
				</div>
				<div className={styles.marketPageHeader__titleWrapper}>
					<h1 className={styles.marketPageHeader__title}>{title}</h1>
					<p className={styles.marketPageHeader__description}>{description}</p>
				</div>
			</div>

			<section
				aria-label="Indicadores del mercado"
				className={styles.marketPageHeader__metrics}
			>
				{metrics
					? metrics.map(
							({
								label,
								value,
								detail,
								tooltip,
								icon: MetricIcon,
								variant,
							}) => (
								<div
									key={label}
									className={`${styles.marketPageHeader__metricItem} ${VARIANT_CLASSES[variant]}`}
									title={tooltip}
								>
									<span
										className={styles.marketPageHeader__metricIconWrapper}
										aria-hidden="true"
									>
										<MetricIcon
											className={styles.marketPageHeader__metricIcon}
										/>
									</span>
									<div className={styles.marketPageHeader__metricBody}>
										<span className={styles.marketPageHeader__metricLabel}>
											{label}
										</span>
										<strong className={styles.marketPageHeader__metricValue}>
											{value}
										</strong>
										<span className={styles.marketPageHeader__metricDetail}>
											{detail}
										</span>
									</div>
								</div>
							),
						)
					: children}
			</section>
		</header>
	);
}
