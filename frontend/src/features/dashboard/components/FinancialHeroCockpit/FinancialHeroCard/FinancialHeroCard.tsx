import type React from "react";
import type { LucideIcon } from "@/shared/icons";
import styles from "./FinancialHeroCard.module.scss";

export type CardAccentTone =
	| "gain"
	| "loss"
	| "highlight"
	| "investment"
	| "blue"
	| "indigo"
	| "violet"
	| "rose"
	| "emerald";

export interface FinancialHeroCardProps {
	title: string;
	value: React.ReactNode;
	subtitle?: string;
	delta?: {
		value: number;
		isPercentage?: boolean;
		label?: string;
	};
	trend?: "up" | "down" | "neutral";
	accentTone?: CardAccentTone;
	icon: LucideIcon;
	sparkline?: React.ReactNode;
	className?: string;
}

const TONE_TO_VARIANT: Record<CardAccentTone, string> = {
	gain: styles.variant_emerald,
	emerald: styles.variant_emerald,
	loss: styles.variant_rose,
	rose: styles.variant_rose,
	highlight: styles.variant_blue,
	blue: styles.variant_blue,
	investment: styles.variant_violet,
	violet: styles.variant_violet,
	indigo: styles.variant_indigo,
};

export function FinancialHeroCard({
	title,
	value,
	subtitle,
	delta,
	trend = "neutral",
	accentTone = "blue",
	icon: Icon,
	sparkline,
	className,
}: FinancialHeroCardProps) {
	const trendClass =
		trend === "up"
			? styles.trendUp
			: trend === "down"
				? styles.trendDown
				: styles.trendNeutral;

	const variantClass = TONE_TO_VARIANT[accentTone] ?? styles.variant_blue;

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
			className={`${styles.card} ${variantClass} ${className ?? ""}`.trim()}
		>
			<span className={styles.tileWrapper} aria-hidden="true">
				<Icon className={styles.icon} />
			</span>

			<div className={styles.contentCol}>
				<header className={styles.header}>
					<span className={styles.title}>{title}</span>
					{delta && (
						<span className={`${styles.deltaBadge} ${trendClass}`}>
							{formatDelta(delta.value, delta.isPercentage)}
							{delta.label && <span> {delta.label}</span>}
						</span>
					)}
				</header>

				<div className={styles.valueRow}>
					<strong className={styles.value}>{value}</strong>
					{sparkline && (
						<div className={styles.sparklineWrapper}>{sparkline}</div>
					)}
				</div>

				{subtitle && <span className={styles.subtitle}>{subtitle}</span>}
			</div>
		</article>
	);
}
