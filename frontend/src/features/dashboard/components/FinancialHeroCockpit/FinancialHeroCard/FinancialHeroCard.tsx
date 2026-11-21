import type React from "react";
import type { LucideIcon } from "@/shared/icons";
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
	accentTone?: "gain" | "loss" | "highlight" | "investment";
	icon: LucideIcon;
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
	accentTone = "highlight",
	icon: Icon,
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

	const accentClass = styles[`accent_${accentTone}`];

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
			className={`${styles.card} ${accentClass} ${isWarning ? styles.cardWarning : ""} ${className ?? ""}`.trim()}
		>
			<div className={styles.cardLayout}>
				<div className={styles.tileWrapper}>
					<span className={styles.iconTile} aria-hidden="true">
						<Icon />
					</span>
				</div>

				<div className={styles.contentCol}>
					<header className={styles.header}>
						<span className={styles.title}>{title}</span>
						{delta && (
							<span className={`${styles.deltaBadge} ${trendClass}`}>
								{formatDelta(delta.value, delta.isPercentage)}
								{delta.label && (
									<span className={styles.deltaLabel}> {delta.label}</span>
								)}
							</span>
						)}
					</header>

					<div className={styles.valueRow}>
						<span className={styles.value}>{value}</span>
						{sparkline && (
							<div className={styles.sparklineContainer}>{sparkline}</div>
						)}
					</div>

					<footer className={styles.footer}>
						{subtitle && <span className={styles.subtitle}>{subtitle}</span>}
						{extraAction && (
							<div className={styles.extraAction}>{extraAction}</div>
						)}
					</footer>
				</div>
			</div>
		</article>
	);
}
