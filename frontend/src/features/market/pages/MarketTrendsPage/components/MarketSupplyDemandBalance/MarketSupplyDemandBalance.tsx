import { TokenSymbol } from "@/shared/components";
import { ArrowRightLeft, ShieldCheck, ShoppingCart, Tag } from "@/shared/icons";
import styles from "./MarketSupplyDemandBalance.module.scss";

export interface MarketSupplyDemandBalanceProps {
	buyPercentage?: number | null;
	sellPercentage?: number | null;
	totalTransactions: number;
	totalTradedCredits: number;
	totalTradedTokens: number;
}

export default function MarketSupplyDemandBalance({
	buyPercentage,
	sellPercentage,
	totalTransactions,
	totalTradedCredits,
	totalTradedTokens,
}: MarketSupplyDemandBalanceProps) {
	const clampedBuy = Math.max(
		0,
		Math.min(100, Math.round(buyPercentage ?? 50)),
	);
	const clampedSell = Math.max(
		0,
		Math.min(100, Math.round(sellPercentage ?? 50)),
	);

	// Estimamos volumen derivado por proporción de compras vs ventas
	const estimatedBuyCredits = (totalTradedCredits * clampedBuy) / 100;
	const estimatedSellCredits = (totalTradedCredits * clampedSell) / 100;
	const estimatedBuyTokens = Math.round((totalTradedTokens * clampedBuy) / 100);
	const estimatedSellTokens = Math.round(
		(totalTradedTokens * clampedSell) / 100,
	);

	const demandToSupplyRatio =
		clampedSell > 0 ? (clampedBuy / clampedSell).toFixed(2) : "∞";

	return (
		<div className={styles.card}>
			<div className={styles.header}>
				<div className={styles.titleGroup}>
					<ArrowRightLeft className={styles.icon} aria-hidden="true" />
					<div>
						<h3 className={styles.title}>Balance de Flujo Oferta vs Demanda</h3>
						<p className={styles.subtitle}>
							Absorción de liquidez compradora frente a presión vendedora
						</p>
					</div>
				</div>

				<div className={styles.ratioPill}>
					<span className={styles.ratioLabel}>
						Ratio Demanda/Oferta ({totalTransactions.toLocaleString("es-AR")}{" "}
						ops):
					</span>
					<strong className={styles.ratioValue}>{demandToSupplyRatio}x</strong>
				</div>
			</div>

			<div className={styles.panelsGrid}>
				{/* Lado Demanda (Compras) */}
				<div className={`${styles.panel} ${styles["panel--demand"]}`}>
					<div className={styles.panelHeader}>
						<div className={styles.panelTag}>
							<ShoppingCart className={styles.panelIcon} aria-hidden="true" />
							<span>Demanda (Compras)</span>
						</div>
						<span className={styles.panelPercent}>{clampedBuy}%</span>
					</div>

					<div className={styles.panelMetrics}>
						<div className={styles.metricRow}>
							<span className={styles.metricLabel}>Volumen comprador:</span>
							<strong className={styles.metricValue}>
								{estimatedBuyCredits.toLocaleString("es-AR", {
									minimumFractionDigits: 2,
									maximumFractionDigits: 2,
								})}{" "}
								₵
							</strong>
						</div>
						<div className={styles.metricRow}>
							<span className={styles.metricLabel}>Tokens absorbidos:</span>
							<strong className={styles.metricValue}>
								{estimatedBuyTokens.toLocaleString("es-AR")}{" "}
								<TokenSymbol size="sm" />
							</strong>
						</div>
					</div>

					<div className={styles.panelStatus}>
						<ShieldCheck className={styles.statusIcon} aria-hidden="true" />
						<span>
							{clampedBuy >= 55
								? "Fuerte presión compradora"
								: clampedBuy <= 45
									? "Demanda retraída"
									: "Demanda equilibrada"}
						</span>
					</div>
				</div>

				{/* Lado Oferta (Ventas) */}
				<div className={`${styles.panel} ${styles["panel--supply"]}`}>
					<div className={styles.panelHeader}>
						<div className={styles.panelTag}>
							<Tag className={styles.panelIcon} aria-hidden="true" />
							<span>Oferta (Ventas)</span>
						</div>
						<span className={styles.panelPercent}>{clampedSell}%</span>
					</div>

					<div className={styles.panelMetrics}>
						<div className={styles.metricRow}>
							<span className={styles.metricLabel}>Volumen vendedor:</span>
							<strong className={styles.metricValue}>
								{estimatedSellCredits.toLocaleString("es-AR", {
									minimumFractionDigits: 2,
									maximumFractionDigits: 2,
								})}{" "}
								₵
							</strong>
						</div>
						<div className={styles.metricRow}>
							<span className={styles.metricLabel}>Tokens ofertados:</span>
							<strong className={styles.metricValue}>
								{estimatedSellTokens.toLocaleString("es-AR")}{" "}
								<TokenSymbol size="sm" />
							</strong>
						</div>
					</div>

					<div className={styles.panelStatus}>
						<ShieldCheck className={styles.statusIcon} aria-hidden="true" />
						<span>
							{clampedSell >= 55
								? "Exceso de oferta circulante"
								: clampedSell <= 45
									? "Oferta contenida"
									: "Oferta estable"}
						</span>
					</div>
				</div>
			</div>

			<div className={styles.depthBar}>
				<div
					className={styles.depthSegmentBuy}
					style={{ width: `${clampedBuy}%` }}
				>
					<span>{clampedBuy}% Compras</span>
				</div>
				<div
					className={styles.depthSegmentSell}
					style={{ width: `${clampedSell}%` }}
				>
					<span>{clampedSell}% Ventas</span>
				</div>
			</div>
		</div>
	);
}
