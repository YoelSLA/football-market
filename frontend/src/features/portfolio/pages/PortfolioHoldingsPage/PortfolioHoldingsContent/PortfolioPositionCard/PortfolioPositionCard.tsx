import { portfolioLabels } from "@/features/portfolio/constants";
import PortfolioPortrait from "@/features/portfolio/pages/PortfolioHoldingsPage/PortfolioPortrait";
import type { PortfolioPosition } from "@/features/portfolio/types";
import { formatCredits } from "@/features/portfolio/utils";
import {
	ChartPie,
	CircleDot,
	Coins,
	Lock,
	type LucideIcon,
	Receipt,
	TrendingUp,
} from "@/shared/icons";
import styles from "./PortfolioPositionCard.module.scss";

type MetricIcon =
	| "tokens"
	| "available"
	| "reserved"
	| "average"
	| "current"
	| "value";

const METRIC_ICONS: Record<MetricIcon, LucideIcon> = {
	tokens: Coins,
	available: CircleDot,
	reserved: Lock,
	average: Receipt,
	current: TrendingUp,
	value: ChartPie,
};

const METRIC_ICON_CLASSES: Record<MetricIcon, string> = {
	tokens: styles.portfolioPosition__icon_tokens,
	available: styles.portfolioPosition__icon_available,
	reserved: styles.portfolioPosition__icon_reserved,
	average: styles.portfolioPosition__icon_average,
	current: styles.portfolioPosition__icon_current,
	value: styles.portfolioPosition__icon_value,
};

const RESULT_CLASSES: Record<string, string> = {
	positive: styles.portfolioPosition__result_positive,
	negative: styles.portfolioPosition__result_negative,
	neutral: styles.portfolioPosition__result_neutral,
};

interface Props {
	position: PortfolioPosition;
	onDetails: () => void;
}

export default function PortfolioPositionCard({
	position: p,
	onDetails,
}: Props) {
	const result = p.totalProfitLoss;
	const resultTone =
		result === null || result === 0
			? "neutral"
			: result > 0
				? "positive"
				: "negative";
	const currentPrice =
		p.valuationStatus === "UNVALUED"
			? "Sin cotización"
			: formatCredits(p.currentUnitPrice, true);

	const tokenMetrics = [
		{
			key: "tokens" as MetricIcon,
			label: "Tokens totales",
			value: String(p.quantity),
		},
		{
			key: "available" as MetricIcon,
			label: "Disponibles",
			value: String(p.availableQuantity),
		},
		{
			key: "reserved" as MetricIcon,
			label: "Reservados",
			value: String(p.reservedQuantity),
		},
	] as const;

	const financialMetrics = [
		{
			key: "average" as MetricIcon,
			label: "Precio promedio",
			value: formatCredits(p.averageUnitPrice, true),
			tone: undefined,
		},
		{
			key: "current" as MetricIcon,
			label: "Precio actual",
			value: currentPrice,
			tone: p.valuationStatus === "UNVALUED" ? "warning" : undefined,
		},
		{
			key: "value" as MetricIcon,
			label: "Valor actual",
			value: formatCredits(p.currentValue, true),
			tone: undefined,
		},
	] as const;

	return (
		<article className={styles.portfolioPosition}>
			<header className={styles.portfolioPosition__header}>
				<PortfolioPortrait imageUrl={p.imageUrl} name={p.playerName} />
				<div className={styles.portfolioPosition__identity}>
					<h2>{p.playerName ?? portfolioLabels.missing}</h2>
					<div className={styles.portfolioPosition__meta}>
						<p>
							<span
								aria-hidden="true"
								className={styles.portfolioPosition__ball}
							>
								⚽&#xFE0E;
							</span>
							{p.teamName ?? portfolioLabels.missing}
						</p>
						<p>{p.position ?? portfolioLabels.missing}</p>
					</div>
				</div>
				<div className={styles.portfolioPosition__headerActions}>
					<span
						className={`${styles.portfolioPosition__status} ${
							p.status === "OPEN"
								? styles.portfolioPosition__status_open
								: styles.portfolioPosition__status_closed
						}`}
					>
						<span
							className={styles.portfolioPosition__statusDot}
							aria-hidden="true"
						/>
						{p.status === "OPEN" ? "ABIERTA" : "CERRADA"}
					</span>
				</div>
			</header>

			<section
				className={styles.portfolioPosition__tokenMetrics}
				aria-label="Métricas de tokens"
			>
				{tokenMetrics.map(({ key, label, value }) => {
					const Icon = METRIC_ICONS[key];
					return (
						<div
							key={key}
							className={`${styles.portfolioPosition__tokenMetric} ${styles[key]}`}
						>
							<span
								className={`${styles.portfolioPosition__tokenIcon} ${styles[key]}`}
							>
								<Icon
									className={styles.portfolioPosition__tokenIconSvg}
									aria-hidden="true"
								/>
							</span>
							<div className={styles.portfolioPosition__tokenInfo}>
								<dt className={styles.portfolioPosition__tokenValue}>
									{value}
								</dt>
								<dd className={styles.portfolioPosition__tokenLabel}>
									{label}
								</dd>
							</div>
						</div>
					);
				})}
			</section>

			<section
				className={styles.portfolioPosition__financialMetrics}
				aria-label="Métricas financieras"
			>
				{financialMetrics.map(({ key, label, value, tone }) => {
					const Icon = METRIC_ICONS[key];
					return (
						<div
							key={key}
							className={[
								styles.portfolioPosition__financialMetric,
								tone && styles.portfolioPosition__metric_warning,
							]
								.filter(Boolean)
								.join(" ")}
						>
							<span
								className={`${styles.portfolioPosition__icon} ${METRIC_ICON_CLASSES[key]}`}
							>
								<Icon aria-hidden="true" />
							</span>
							<div className={styles.portfolioPosition__financialInfo}>
								<dt className={styles.portfolioPosition__financialValue}>
									{value}
								</dt>
								<dd className={styles.portfolioPosition__financialLabel}>
									{label}
								</dd>
							</div>
						</div>
					);
				})}
			</section>

			<footer className={styles.portfolioPosition__footer}>
				<span
					className={`${styles.portfolioPosition__result} ${RESULT_CLASSES[resultTone]}`}
				>
					<svg
						className={styles.portfolioPosition__resultIcon}
						aria-hidden="true"
						viewBox="0 0 24 24"
						width="16"
						height="16"
					>
						<polyline
							points="23 6 9 18 4 13"
							fill="none"
							stroke="currentColor"
							strokeWidth="2"
							strokeLinecap="round"
							strokeLinejoin="round"
						/>
					</svg>
					<span className={styles.portfolioPosition__resultValue}>
						Resultado: {formatCredits(result, true)}
					</span>
				</span>
				<button
					type="button"
					className={styles.portfolioPosition__details}
					onClick={onDetails}
					aria-label={`Ver detalles de ${p.playerName ?? `jugador ${p.playerId}`}`}
				>
					Ver detalles
					<svg
						className={styles.portfolioPosition__arrowIcon}
						aria-hidden="true"
						viewBox="0 0 24 24"
						width="16"
						height="16"
					>
						<path
							d="M5 12h14M12 5l7 7-7 7"
							fill="none"
							stroke="currentColor"
							strokeWidth="2"
							strokeLinecap="round"
							strokeLinejoin="round"
						/>
					</svg>
				</button>
			</footer>
		</article>
	);
}
