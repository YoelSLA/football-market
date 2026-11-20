import type {
	PortfolioMetricIcon,
	PortfolioMetricTone,
} from "@/features/portfolio/types";
import {
	Database,
	TrendingDown,
	TrendingUp,
	UsersRound,
	Wallet,
} from "@/shared/icons";
import styles from "./PortfolioMetricCard.module.scss";

interface Props {
	label: string;
	value: string;
	description?: string;
	icon: PortfolioMetricIcon;
	tone?: PortfolioMetricTone;
	result?: number | null;
}

const ICONS = {
	value: Wallet,
	invested: Database,
	profit: TrendingUp,
	positions: UsersRound,
};

export default function PortfolioMetricCard({
	label,
	value,
	description,
	icon,
	tone = "current",
	result,
}: Props) {
	const negative = icon === "profit" && result != null && result < 0;
	const Icon = negative ? TrendingDown : ICONS[icon];

	const toneClass = {
		current: styles.portfolioMetric_current,
		invested: styles.portfolioMetric_invested,
		profit: styles.portfolioMetric_profit,
		positions: styles.portfolioMetric_positions,
	}[tone];
	const Indicator =
		icon === "positions" ? UsersRound : negative ? TrendingDown : TrendingUp;
	// Conservar el texto formateado; solo separar visualmente su unidad.
	const creditUnit = " créditos";
	const accentedUnit = tone === "current" && value.endsWith(creditUnit);

	return (
		<article
			className={`${styles.portfolioMetric} ${toneClass} ${negative ? styles.portfolioMetric_negative : ""}`.trim()}
		>
			<span className={styles.portfolioMetric__tile}>
				<Icon className={styles.portfolioMetric__icon} aria-hidden="true" />
			</span>

			<h2 className={styles.portfolioMetric__label}>{label}</h2>

			<strong className={styles.portfolioMetric__value}>
				{accentedUnit ? (
					<>
						<span className={styles.portfolioMetric__amount}>
							{value.slice(0, -creditUnit.length)}
						</span>
						{creditUnit}
					</>
				) : (
					value
				)}
			</strong>

			{description && (
				<p className={styles.portfolioMetric__description}>{description}</p>
			)}
			<Indicator
				className={styles.portfolioMetric__indicator}
				aria-hidden="true"
			/>
		</article>
	);
}
