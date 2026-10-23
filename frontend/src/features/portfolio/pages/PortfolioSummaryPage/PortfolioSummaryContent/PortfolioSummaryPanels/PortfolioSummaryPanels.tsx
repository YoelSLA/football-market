import { Link } from "react-router-dom";
import { PortfolioValueSection } from "@/features/portfolio/components";
import type { PortfolioSummary } from "@/features/portfolio/types";
import { formatCredits, formatPercentage } from "@/features/portfolio/utils";
import {
	ArrowRight,
	BarChart3,
	ChartPie,
	CheckCircle2,
	Clock,
	Coins,
	List,
	Lock,
	Package,
	ShoppingCart,
	Sigma,
	Trophy,
	UsersRound,
	Zap,
} from "@/shared/icons";
import styles from "./PortfolioSummaryPanels.module.scss";
import PortfolioTokenDistribution from "./PortfolioTokenDistribution";

interface Props {
	summary: PortfolioSummary;
}

export default function PortfolioSummaryPanels({ summary }: Props) {
	const operations =
		summary.totalBuyTransactions + summary.totalSellTransactions;

	return (
		<>
			<div className={styles.portfolioSummaryPanels}>
				<PortfolioValueSection
					appearance="dashboard"
					dividedAside
					title="Resumen de tenencias"
					icon={Package}
					rows={[
						{
							label: "Posiciones abiertas",
							value: summary.totalPositions,
							icon: UsersRound,
							iconTone: "cyan",
							iconTile: true,
						},
						{
							label: "Tokens totales",
							value: summary.totalTokens,
							icon: Coins,
							iconTone: "gold",
							iconTile: true,
						},
						{
							label: "Tokens disponibles",
							value: summary.totalAvailableTokens,
							icon: Package,
							iconTone: "green",
							iconTile: true,
						},
						{
							label: "Tokens reservados",
							value: summary.totalReservedTokens,
							icon: Lock,
							iconTone: "violet",
							iconTile: true,
						},
					]}
					aside={
						<PortfolioTokenDistribution
							total={summary.totalTokens}
							available={summary.totalAvailableTokens}
							reserved={summary.totalReservedTokens}
						/>
					}
				/>
				<PortfolioValueSection
					appearance="dashboard"
					title="Resultados financieros"
					icon={BarChart3}
					rows={[
						{
							label: "Resultado realizado",
							value: formatCredits(summary.totalRealizedProfitLoss),
							result: summary.totalRealizedProfitLoss,
							icon: CheckCircle2,
							iconTone: "green",
						},
						{
							label: "Resultado no realizado",
							value: formatCredits(summary.totalUnrealizedProfitLoss),
							result: summary.totalUnrealizedProfitLoss,
							icon: Clock,
							iconTone: "gold",
						},
						{
							label: "Resultado total",
							value: formatCredits(summary.totalProfitLoss),
							result: summary.totalProfitLoss,
							icon: Sigma,
							iconTone: "violet",
							emphasis: true,
						},
						{
							label: "Rentabilidad total",
							value: formatPercentage(summary.totalProfitLossPercentage),
							result: summary.totalProfitLoss,
							icon: ChartPie,
						},
					]}
					aside={
						<div
							className={`${styles.portfolioSummaryPanels__valuation} ${summary.totalProfitLossPercentage !== null && summary.totalProfitLossPercentage < 0 ? styles.portfolioSummaryPanels__valuation_negative : ""}`.trim()}
						>
							<div className={styles.portfolioSummaryPanels__valuationHeading}>
								<span className={styles.portfolioSummaryPanels__valuationIcon}>
									<Trophy aria-hidden="true" />
								</span>
								<div>
									<h3>Valoración actual</h3>
									<strong>{formatCredits(summary.totalCurrentValue)}</strong>
								</div>
							</div>
							<p>Variación respecto al capital invertido</p>
							<strong className={styles.portfolioSummaryPanels__variation}>
								{formatPercentage(summary.totalProfitLossPercentage)}
							</strong>
							<div
								className={styles.portfolioSummaryPanels__variationTrack}
								aria-hidden="true"
							>
								<span
									style={{
										width: `${Math.min(100, Math.abs(summary.totalProfitLossPercentage ?? 0))}%`,
									}}
								/>
							</div>
						</div>
					}
				/>
			</div>
			<div className={styles.portfolioSummaryPanels}>
				<PortfolioValueSection
					appearance="dashboard"
					title="Actividad"
					icon={List}
					rows={[
						{
							label: "Compras realizadas",
							value: summary.totalBuyTransactions,
							icon: ShoppingCart,
							iconTone: "green",
							iconTile: true,
						},
						{
							label: "Ventas realizadas",
							value: summary.totalSellTransactions,
							icon: ShoppingCart,
							iconTone: "gold",
							iconTile: true,
						},
					]}
					aside={
						<div className={styles.portfolioSummaryPanels__activity}>
							<span className={styles.portfolioSummaryPanels__activityIcon}>
								<Zap aria-hidden="true" />
							</span>
							<div>
								<p>Operaciones registradas</p>
								<strong>{operations} en total</strong>
							</div>
						</div>
					}
				/>
				<PortfolioValueSection
					appearance="dashboard"
					action
					title="Explorá tus tenencias"
					icon={ArrowRight}
					body="Consultá tus jugadores, precios y resultados individuales."
					footer={
						<Link
							className={styles.portfolioSummaryPanels__action}
							to="/portfolio/holdings"
						>
							Ver mis tenencias <ArrowRight aria-hidden="true" />
						</Link>
					}
				/>
			</div>
		</>
	);
}
