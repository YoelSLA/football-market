import type { MarketBias } from "@/features/market/types";
import styles from "./MarketSentimentGauge.module.scss";

export interface MarketSentimentGaugeProps {
	buyPercentage?: number | null;
	bias?: MarketBias | null;
}

export default function MarketSentimentGauge({
	buyPercentage,
	bias,
}: MarketSentimentGaugeProps) {
	const clampedBuy = Math.max(
		0,
		Math.min(100, Math.round(buyPercentage ?? 50)),
	);
	const effectiveBias: MarketBias = bias ?? "NEUTRAL";

	// Ángulo en radianes: 0% compra -> 180° (izquierda), 50% compra -> 90° (arriba), 100% compra -> 0° (derecha)
	const angleDeg = 180 - clampedBuy * 1.8;
	const angleRad = (angleDeg * Math.PI) / 180;

	const cx = 120;
	const cy = 115;
	const needleLength = 68;
	const needleWidth = 4;
	const needleTail = 10;

	// Vértices de la aguja poligonal
	const nx = cx + needleLength * Math.cos(angleRad);
	const ny = cy - needleLength * Math.sin(angleRad);

	const bx1 = cx - needleWidth * Math.sin(angleRad);
	const by1 = cy - needleWidth * Math.cos(angleRad);

	const bx2 = cx + needleWidth * Math.sin(angleRad);
	const by2 = cy + needleWidth * Math.cos(angleRad);

	const tx = cx - needleTail * Math.cos(angleRad);
	const ty = cy + needleTail * Math.sin(angleRad);

	const needlePoints = `${bx1.toFixed(1)},${by1.toFixed(1)} ${nx.toFixed(1)},${ny.toFixed(1)} ${bx2.toFixed(1)},${by2.toFixed(1)} ${tx.toFixed(1)},${ty.toFixed(1)}`;

	return (
		<section
			className={styles.gaugeContainer}
			aria-label="Tacómetro de sentimiento del mercado"
		>
			<div className={styles.gaugeContainer__header}>
				<h3 className={styles.gaugeContainer__title}>Sentimiento Radial</h3>
				<span
					className={`${styles.gaugeContainer__bias} ${
						styles[`gaugeContainer__bias--${effectiveBias.toLowerCase()}`]
					}`}
				>
					{effectiveBias === "BULLISH"
						? "ALCISTA"
						: effectiveBias === "BEARISH"
							? "BAJISTA"
							: "NEUTRAL"}
				</span>
			</div>

			<div className={styles.gaugeWrapper}>
				<svg
					viewBox="0 0 240 135"
					className={styles.svg}
					aria-hidden="true"
					focusable="false"
				>
					<defs>
						<linearGradient
							id="gaugeSentimentGradient"
							x1="0%"
							y1="0%"
							x2="100%"
							y2="0%"
						>
							<stop offset="0%" stopColor="#f87171" />
							<stop offset="45%" stopColor="#fbbf24" />
							<stop offset="55%" stopColor="#94a3b8" />
							<stop offset="100%" stopColor="#22e2a2" />
						</linearGradient>
					</defs>

					{/* Arco guía de fondo */}
					<path
						d="M 35 115 A 85 85 0 0 1 205 115"
						fill="none"
						stroke="#1e293b"
						strokeWidth="16"
						strokeLinecap="round"
					/>

					{/* Arco degradado de sentimiento */}
					<path
						d="M 35 115 A 85 85 0 0 1 205 115"
						fill="none"
						stroke="url(#gaugeSentimentGradient)"
						strokeWidth="12"
						strokeLinecap="round"
					/>

					{/* Marcas de referencia */}
					<text x="30" y="130" className={styles.axisLabel} textAnchor="start">
						0% Venta
					</text>
					<text
						x="120"
						y="24"
						className={styles.axisLabelCenter}
						textAnchor="middle"
					>
						50%
					</text>
					<text x="210" y="130" className={styles.axisLabel} textAnchor="end">
						100% Compra
					</text>

					{/* Aguja dinámica con animación suave */}
					<polygon points={needlePoints} className={styles.needle} />
					<circle
						cx={cx}
						cy={cy}
						r="6"
						className={styles.pivot}
						strokeWidth="2"
					/>
				</svg>

				<div className={styles.readout}>
					<span className={styles.readout__value}>{clampedBuy}%</span>
					<span className={styles.readout__label}>Presión Compradora</span>
				</div>
			</div>
		</section>
	);
}
