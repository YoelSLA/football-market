import { Coins, Package, TrendingUp, Wallet } from "@/shared/icons";
import type { FinancialSummary } from "../../types/models";
import { FinancialHeroCard } from "./FinancialHeroCard/FinancialHeroCard";
import styles from "./FinancialHeroCockpit.module.scss";
import { PatrimonySparkline } from "./PatrimonySparkline/PatrimonySparkline";

export interface FinancialHeroCockpitProps {
	summary: FinancialSummary;
	sparklineData?: number[];
	className?: string;
}

function formatCredits(amount: number | null): string {
	if (amount === null) return "—";
	return `${amount.toLocaleString("es-ES", {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	})} ₵`;
}

export function FinancialHeroCockpit({
	summary,
	sparklineData,
	className,
}: FinancialHeroCockpitProps) {
	const pnl = summary.totalProfitLoss ?? 0;
	const pnlPercentage = summary.totalProfitLossPercentage ?? 0;
	const pnlTrend = pnl > 0 ? "up" : pnl < 0 ? "down" : "neutral";

	const unrealizedText =
		summary.totalUnrealizedProfitLoss !== null
			? formatCredits(summary.totalUnrealizedProfitLoss)
			: "0,00 ₵";
	const realizedText = formatCredits(summary.totalRealizedProfitLoss);

	const portfolioSubtitle =
		summary.totalTokens === 0
			? "Sin futbolistas en cartera"
			: `${summary.totalTokens} tokens de futbolistas en propiedad${
					summary.reservedTokens > 0
						? ` (${summary.reservedTokens} a la venta)`
						: ""
				}`;

	const pnlSubtitle =
		summary.totalProfitLoss === null
			? "Sin operaciones registradas"
			: `En cartera: ${unrealizedText} · En ventas: ${realizedText}`;

	return (
		<section
			className={`${styles.cockpitGrid} ${className ?? ""}`.trim()}
			aria-label="Resumen ejecutivo patrimonial"
		>
			{/* 1. Patrimonio Neto */}
			<FinancialHeroCard
				title="Patrimonio Neto"
				icon={Wallet}
				accentTone="gain"
				value={
					summary.valuationComplete
						? formatCredits(summary.netWorth)
						: "Calculando..."
				}
				subtitle="Créditos disponibles + valor de tus futbolistas"
				delta={
					summary.totalProfitLossPercentage !== null
						? {
								value: summary.totalProfitLossPercentage,
								isPercentage: true,
							}
						: undefined
				}
				trend={pnlTrend}
				sparkline={
					<PatrimonySparkline
						data={sparklineData}
						trend={pnlTrend}
						width={85}
						height={28}
					/>
				}
			/>

			{/* 2. Ganancia / Pérdida */}
			<FinancialHeroCard
				title="Ganancia / Pérdida"
				icon={TrendingUp}
				accentTone={pnl >= 0 ? "gain" : "loss"}
				value={formatCredits(summary.totalProfitLoss)}
				subtitle={pnlSubtitle}
				delta={{
					value: pnlPercentage,
					isPercentage: true,
				}}
				trend={pnlTrend}
			/>

			{/* 3. Saldo Disponible */}
			<FinancialHeroCard
				title="Saldo Disponible"
				icon={Coins}
				accentTone="highlight"
				value={formatCredits(summary.availableCredits)}
				subtitle="Créditos libres para operar en el mercado"
				trend="neutral"
			/>

			{/* 4. Valor de Cartera */}
			<FinancialHeroCard
				title="Valor de Cartera"
				icon={Package}
				accentTone="investment"
				value={
					summary.valuationComplete
						? formatCredits(summary.totalPortfolioValue)
						: "Calculando..."
				}
				subtitle={portfolioSubtitle}
				trend="neutral"
			/>
		</section>
	);
}
