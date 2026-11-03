import type { PortfolioSummary } from "@/features/portfolio/types";
import { formatCredits, formatPercentage } from "@/features/portfolio/utils";
import PortfolioMetricCard from "./PortfolioMetricCard";

import styles from "./PortfolioSummaryMetrics.module.scss";

interface Props {
	summary: PortfolioSummary;
}

export default function PortfolioSummaryMetrics({ summary }: Props) {
	return (
		<div className={styles.portfolioSummaryMetrics}>
			<PortfolioMetricCard
				label="Valor actual del portfolio"
				value={formatCredits(summary.totalCurrentValue)}
				description="Valoración actual"
				icon="value"
				tone="current"
			/>
			<PortfolioMetricCard
				label="Capital invertido"
				value={formatCredits(summary.totalInvested)}
				description="Costo de adquisición"
				icon="invested"
				tone="invested"
			/>
			<PortfolioMetricCard
				label="Resultado total"
				value={formatCredits(summary.totalProfitLoss)}
				description={`${formatPercentage(summary.totalProfitLossPercentage)} de rentabilidad`}
				icon="profit"
				tone="profit"
				result={summary.totalProfitLoss}
			/>
			<PortfolioMetricCard
				label="Posiciones abiertas"
				value={String(summary.totalPositions)}
				description="Tenencia registrada"
				icon="positions"
				tone="positions"
			/>
		</div>
	);
}
