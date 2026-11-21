import { Link } from "react-router-dom";
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
			: "—";
	const realizedText = formatCredits(summary.totalRealizedProfitLoss);

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
				subtitle="Valoración integral de cartera y saldo"
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
						width={80}
						height={32}
					/>
				}
			/>

			{/* 2. P&L Consolidado */}
			<FinancialHeroCard
				title="P&L Consolidado"
				icon={TrendingUp}
				accentTone={pnl >= 0 ? "gain" : "loss"}
				value={formatCredits(summary.totalProfitLoss)}
				subtitle={`No real.: ${unrealizedText} · Real.: ${realizedText}`}
				delta={{
					value: pnlPercentage,
					isPercentage: true,
					label: "total",
				}}
				trend={pnlTrend}
			/>

			{/* 3. Liquidez Disponible */}
			<FinancialHeroCard
				title="Liquidez Disponible"
				icon={Coins}
				accentTone="highlight"
				value={formatCredits(summary.availableCredits)}
				subtitle="Efectivo disponible para adquisiciones"
				trend="neutral"
				extraAction={
					<Link to="/market/buy" className={styles.actionLink}>
						Operar →
					</Link>
				}
			/>

			{/* 4. Custodia de Tokens */}
			<FinancialHeroCard
				title="Custodia de Tokens"
				icon={Package}
				accentTone="investment"
				value={`${summary.totalTokens} Tokens`}
				subtitle={`${summary.availableTokens} libres · ${summary.reservedTokens} en venta P2P`}
				trend="neutral"
				extraAction={
					<Link to="/portfolio" className={styles.actionLink}>
						Cartera →
					</Link>
				}
			/>
		</section>
	);
}
