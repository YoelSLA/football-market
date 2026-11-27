import type { PortfolioSummary } from "@/features/portfolio/types";
import { formatCredits, formatPercentage } from "@/features/portfolio/utils";
import {
	BarChart3,
	Package,
	TrendingDown,
	TrendingUp,
	Wallet,
} from "@/shared/icons";
import styles from "./PortfolioCockpit.module.scss";

interface Props {
	summary: PortfolioSummary;
	isLoading?: boolean;
}

const VARIANT_CLASSES = {
	blue: styles.portfolioCockpit__metric_blue,
	violet: styles.portfolioCockpit__metric_violet,
	indigo: styles.portfolioCockpit__metric_indigo,
	rose: styles.portfolioCockpit__metric_rose,
} as const;

export default function PortfolioCockpit({ summary, isLoading }: Props) {
	const isProfitPositive =
		summary.totalProfitLoss !== null && summary.totalProfitLoss > 0;
	const isProfitNegative =
		summary.totalProfitLoss !== null && summary.totalProfitLoss < 0;

	const profitValueToneClass = isProfitPositive
		? styles.portfolioCockpit__value_positive
		: isProfitNegative
			? styles.portfolioCockpit__value_negative
			: "";

	const profitDetailToneClass = isProfitPositive
		? styles.portfolioCockpit__detail_positive
		: isProfitNegative
			? styles.portfolioCockpit__detail_negative
			: "";

	return (
		<section
			className={styles.portfolioCockpit}
			aria-label="Resumen patrimonial del portfolio"
		>
			{/* Card 1: Valor actual del portfolio */}
			<div
				className={`${styles.portfolioCockpit__metricItem} ${VARIANT_CLASSES.blue}`}
				title={`Valor total actual: ${formatCredits(summary.totalCurrentValue)} · Costo invertido: ${formatCredits(summary.totalInvested)}`}
			>
				<span
					className={styles.portfolioCockpit__metricIconWrapper}
					aria-hidden="true"
				>
					<Wallet className={styles.portfolioCockpit__metricIcon} />
				</span>
				<div className={styles.portfolioCockpit__metricBody}>
					<span className={styles.portfolioCockpit__metricLabel}>
						Valor del Portfolio
					</span>
					<strong className={styles.portfolioCockpit__metricValue}>
						{isLoading ? "—" : formatCredits(summary.totalCurrentValue)}
					</strong>
					<span className={styles.portfolioCockpit__metricDetail}>
						Costo invertido:{" "}
						{isLoading ? "—" : formatCredits(summary.totalInvested)}
					</span>
				</div>
			</div>

			{/* Card 2: Resultado Total (PnL) */}
			<div
				className={`${styles.portfolioCockpit__metricItem} ${VARIANT_CLASSES.indigo}`}
				title={`Resultado total acumulado: ${formatCredits(summary.totalProfitLoss, true)} (${formatPercentage(summary.totalProfitLossPercentage)})`}
			>
				<span
					className={styles.portfolioCockpit__metricIconWrapper}
					aria-hidden="true"
				>
					{isProfitNegative ? (
						<TrendingDown className={styles.portfolioCockpit__metricIcon} />
					) : (
						<TrendingUp className={styles.portfolioCockpit__metricIcon} />
					)}
				</span>
				<div className={styles.portfolioCockpit__metricBody}>
					<span className={styles.portfolioCockpit__metricLabel}>
						Resultado Total
					</span>
					<strong
						className={`${styles.portfolioCockpit__metricValue} ${profitValueToneClass}`}
					>
						{isLoading ? "—" : formatCredits(summary.totalProfitLoss, true)}
					</strong>
					<span
						className={`${styles.portfolioCockpit__metricDetail} ${profitDetailToneClass}`}
					>
						{isLoading
							? "—"
							: `${formatPercentage(summary.totalProfitLossPercentage)} rentabilidad`}
					</span>
				</div>
			</div>

			{/* Card 3: Desglose de PnL (Realizado vs Latente) */}
			<div
				className={`${styles.portfolioCockpit__metricItem} ${VARIANT_CLASSES.violet}`}
				title={`Ganancia/Pérdida realizada: ${formatCredits(summary.totalRealizedProfitLoss, true)} · Latente: ${formatCredits(summary.totalUnrealizedProfitLoss, true)}`}
			>
				<span
					className={styles.portfolioCockpit__metricIconWrapper}
					aria-hidden="true"
				>
					<BarChart3 className={styles.portfolioCockpit__metricIcon} />
				</span>
				<div className={styles.portfolioCockpit__metricBody}>
					<span className={styles.portfolioCockpit__metricLabel}>
						Desglose PnL
					</span>
					<div className={styles.portfolioCockpit__breakdown}>
						<div className={styles.portfolioCockpit__breakdownRow}>
							<span className={styles.portfolioCockpit__breakdownLabel}>
								Realizado:
							</span>
							<strong
								className={`${styles.portfolioCockpit__breakdownValue} ${
									summary.totalRealizedProfitLoss > 0
										? styles.portfolioCockpit__value_positive
										: summary.totalRealizedProfitLoss < 0
											? styles.portfolioCockpit__value_negative
											: ""
								}`}
							>
								{isLoading
									? "—"
									: formatCredits(summary.totalRealizedProfitLoss, true)}
							</strong>
						</div>
						<div className={styles.portfolioCockpit__breakdownRow}>
							<span className={styles.portfolioCockpit__breakdownLabel}>
								Latente:
							</span>
							<strong
								className={`${styles.portfolioCockpit__breakdownValue} ${
									summary.totalUnrealizedProfitLoss !== null &&
									summary.totalUnrealizedProfitLoss > 0
										? styles.portfolioCockpit__value_positive
										: summary.totalUnrealizedProfitLoss !== null &&
												summary.totalUnrealizedProfitLoss < 0
											? styles.portfolioCockpit__value_negative
											: ""
								}`}
							>
								{isLoading
									? "—"
									: formatCredits(summary.totalUnrealizedProfitLoss, true)}
							</strong>
						</div>
					</div>
				</div>
			</div>

			{/* Card 4: Posiciones y Tokens en cartera */}
			<div
				className={`${styles.portfolioCockpit__metricItem} ${VARIANT_CLASSES.rose}`}
				title={`${summary.totalPositions} posiciones · ${summary.totalTokens} tokens (${summary.totalAvailableTokens} disponibles, ${summary.totalReservedTokens} en venta)`}
			>
				<span
					className={styles.portfolioCockpit__metricIconWrapper}
					aria-hidden="true"
				>
					<Package className={styles.portfolioCockpit__metricIcon} />
				</span>
				<div className={styles.portfolioCockpit__metricBody}>
					<span className={styles.portfolioCockpit__metricLabel}>
						Activos en Cartera
					</span>
					<strong className={styles.portfolioCockpit__metricValue}>
						{isLoading ? (
							"—"
						) : (
							<>
								{summary.totalPositions}{" "}
								<span className={styles.portfolioCockpit__metricUnit}>
									{summary.totalPositions === 1 ? "posición" : "posiciones"}
								</span>
							</>
						)}
					</strong>
					<span className={styles.portfolioCockpit__metricDetail}>
						{isLoading
							? "—"
							: `${summary.totalTokens} tokens (${summary.totalAvailableTokens} disp. / ${summary.totalReservedTokens} en venta)`}
					</span>
				</div>
			</div>
		</section>
	);
}
