import type React from "react";
import styles from "./FinancialHeroCard.module.scss";

export interface FinancialHeroCardProps {
	title: string;
	value: string | number;
	subtitle?: string;
	delta?: {
		value: number;
		isPercentage?: boolean;
		label?: string;
	};
	trend?: "up" | "down" | "neutral";
	icon?: React.ReactNode;
	extraAction?: React.ReactNode;
	sparkline?: React.ReactNode;
	isWarning?: boolean;
	className?: string;
}

export function FinancialHeroCard({
	title,
	value,
	subtitle,
	delta,
	trend = "neutral",
	icon,
	extraAction,
	sparkline,
	isWarning,
	className,
}: FinancialHeroCardProps) {
	const trendClass =
		trend === "up"
			? styles.trendUp
			: trend === "down"
				? styles.trendDown
				: styles.trendNeutral;

	const formatDelta = (val: number, isPct?: boolean) => {
		const sign = val > 0 ? "+" : "";
		const formatted = val.toLocaleString("es-ES", {
			minimumFractionDigits: 2,
			maximumFractionDigits: 2,
		});
		return `${sign}${formatted}${isPct ? "%" : ""}`;
	};

	return (
		<article
			className={`${styles.card} ${isWarning ? styles.cardWarning : ""} ${className ?? ""}`.trim()}
		>
			<header className={styles.header}>
				<span className={styles.title}>{title}</span>
				{icon && <span className={styles.iconContainer}>{icon}</span>}
			</header>

			<div className={styles.mainContent}>
				<div className={styles.valueRow}>
					<span className={styles.value}>{value}</span>
					{delta && (
						<span className={`${styles.deltaBadge} ${trendClass}`}>
							{formatDelta(delta.value, delta.isPercentage)}
							{delta.label && (
								<span className={styles.deltaLabel}> {delta.label}</span>
							)}
						</span>
					)}
				</div>

				{sparkline && (
					<div className={styles.sparklineContainer}>{sparkline}</div>
				)}
			</div>

			<footer className={styles.footer}>
				{subtitle && <span className={styles.subtitle}>{subtitle}</span>}
				{extraAction && <div className={styles.extraAction}>{extraAction}</div>}
			</footer>
		</article>
	);
}
