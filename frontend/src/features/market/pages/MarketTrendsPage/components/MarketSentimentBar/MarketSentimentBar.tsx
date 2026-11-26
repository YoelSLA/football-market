import type { MarketBias } from "@/features/market/types";
import { Activity, TrendingDown, TrendingUp } from "@/shared/icons";
import styles from "./MarketSentimentBar.module.scss";

export interface MarketSentimentBarProps {
	buyPercentage?: number | null;
	sellPercentage?: number | null;
	bias?: MarketBias | null;
	totalTransactions: number;
}

export default function MarketSentimentBar({
	buyPercentage,
	sellPercentage,
	bias,
	totalTransactions,
}: MarketSentimentBarProps) {
	const clampedBuy = Math.max(
		0,
		Math.min(100, Math.round(buyPercentage ?? 50)),
	);
	const clampedSell = Math.max(
		0,
		Math.min(100, Math.round(sellPercentage ?? 50)),
	);
	const effectiveBias: MarketBias = bias ?? "NEUTRAL";

	const biasConfig = {
		BULLISH: {
			label: "Mercado Alcista",
			icon: TrendingUp,
			className: styles["bias--bullish"],
		},
		BEARISH: {
			label: "Mercado Bajista",
			icon: TrendingDown,
			className: styles["bias--bearish"],
		},
		NEUTRAL: {
			label: "Mercado Neutral",
			icon: Activity,
			className: styles["bias--neutral"],
		},
	}[effectiveBias];

	const BiasIcon = biasConfig.icon;

	return (
		<section
			className={styles.container}
			aria-label="Presión de compra vs venta"
		>
			<div className={styles.header}>
				<div>
					<h3 className={styles.title}>Presión de Compra vs Venta</h3>
					<p className={styles.subtitle}>
						Proporción de órdenes ejecutadas en las últimas 24 horas
					</p>
				</div>
				<div className={`${styles.biasBadge} ${biasConfig.className}`}>
					<BiasIcon className={styles.biasIcon} aria-hidden="true" />
					<span>{biasConfig.label}</span>
				</div>
			</div>

			<div className={styles.barWrapper}>
				<div
					className={styles.segmentBuy}
					style={{ width: `${clampedBuy}%` }}
					title={`Compras: ${clampedBuy}%`}
				>
					{clampedBuy >= 15 && (
						<span className={styles.segmentLabel}>{clampedBuy}% Compra</span>
					)}
				</div>
				<div
					className={styles.segmentSell}
					style={{ width: `${clampedSell}%` }}
					title={`Ventas: ${clampedSell}%`}
				>
					{clampedSell >= 15 && (
						<span className={styles.segmentLabel}>{clampedSell}% Venta</span>
					)}
				</div>
			</div>

			<div className={styles.footer}>
				<div className={styles.legend}>
					<span className={styles.legendBuy}>
						<span className={styles.dotBuy} /> {clampedBuy}% Compras
					</span>
					<span className={styles.legendSell}>
						<span className={styles.dotSell} /> {clampedSell}% Ventas
					</span>
				</div>
				<span className={styles.transactions}>
					{totalTransactions.toLocaleString("es-AR")} operaciones totales
				</span>
			</div>
		</section>
	);
}
