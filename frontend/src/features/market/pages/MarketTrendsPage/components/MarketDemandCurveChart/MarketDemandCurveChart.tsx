import type { MarketBias } from "@/features/market/types";
import { TokenSymbol } from "@/shared/components";
import { TrendingUp, Zap } from "@/shared/icons";
import styles from "./MarketDemandCurveChart.module.scss";

export interface MarketDemandCurveChartProps {
	buyPercentage?: number | null;
	bias?: MarketBias | null;
}

export default function MarketDemandCurveChart({
	buyPercentage,
	bias = "NEUTRAL",
}: MarketDemandCurveChartProps) {
	const clampedBuy = Math.max(
		0,
		Math.min(100, Math.round(buyPercentage ?? 50)),
	);

	// Normalizamos el factor de demanda (-1 a +1 respecto al centro de 50%)
	const normalizedDemand = (clampedBuy - 50) / 50;
	const estimatedImpactPercent = normalizedDemand * 50;

	// Coordenadas en viewBox 0 0 360 180
	// Eje X: 40px (0%) a 320px (100%)
	const currentX = 40 + (clampedBuy / 100) * 280;
	// Curva sigmoide para Y: y = 90 - (normalizedDemand * 60)
	const currentY = 90 - normalizedDemand * 55;

	return (
		<div className={styles.card}>
			<div className={styles.header}>
				<div className={styles.titleGroup}>
					<Zap className={styles.icon} aria-hidden="true" />
					<div>
						<h3 className={styles.title}>
							Curva de Elasticidad Oferta vs Demanda
						</h3>
						<p className={styles.subtitle}>
							Sensibilidad de la cotización según la presión compradora neta
						</p>
					</div>
				</div>

				<div className={styles.badge}>
					<TrendingUp className={styles.badgeIcon} aria-hidden="true" />
					<span>
						{estimatedImpactPercent >= 0 ? "+" : ""}
						{estimatedImpactPercent.toFixed(1)}% Impacto estimado
					</span>
				</div>
			</div>

			<div className={styles.chartWrapper}>
				<svg
					viewBox="0 0 360 180"
					className={styles.svg}
					aria-label="Curva de oferta y demanda con impacto en cotización"
				>
					<defs>
						<linearGradient
							id="demandCurveGradient"
							x1="0%"
							y1="0%"
							x2="100%"
							y2="0%"
						>
							<stop offset="0%" stopColor="#ef4444" />
							<stop offset="45%" stopColor="#f59e0b" />
							<stop offset="55%" stopColor="#94a3b8" />
							<stop offset="100%" stopColor="#10b981" />
						</linearGradient>

						<linearGradient
							id="demandAreaGradient"
							x1="0%"
							y1="0%"
							x2="0%"
							y2="100%"
						>
							<stop offset="0%" stopColor="#10b981" stopOpacity="0.25" />
							<stop offset="100%" stopColor="#03172c" stopOpacity="0.0" />
						</linearGradient>
					</defs>

					{/* Guías de referencia */}
					<line
						x1="40"
						y1="90"
						x2="320"
						y2="90"
						className={styles.equilibriumLine}
					/>
					<line
						x1="180"
						y1="25"
						x2="180"
						y2="155"
						className={styles.centerLine}
					/>

					{/* Rótulos de ejes */}
					<text x="35" y="32" className={styles.axisLabel} textAnchor="end">
						+50%
					</text>
					<text x="35" y="94" className={styles.axisLabel} textAnchor="end">
						1.0x
					</text>
					<text x="35" y="156" className={styles.axisLabel} textAnchor="end">
						-50%
					</text>

					<text
						x="40"
						y="172"
						className={styles.subAxisLabel}
						textAnchor="start"
					>
						Exceso Oferta
					</text>
					<text
						x="180"
						y="172"
						className={styles.subAxisLabel}
						textAnchor="middle"
					>
						Equilibrio
					</text>
					<text
						x="320"
						y="172"
						className={styles.subAxisLabel}
						textAnchor="end"
					>
						Alta Demanda
					</text>

					{/* Curva de Oferta/Demanda */}
					<path
						d="M 40 145 C 130 145, 140 90, 180 90 C 220 90, 230 35, 320 35"
						fill="none"
						stroke="url(#demandCurveGradient)"
						strokeWidth="4"
						strokeLinecap="round"
					/>

					{/* Línea vertical proyectada desde el punto actual */}
					<line
						x1={currentX}
						y1={currentY}
						x2={currentX}
						y2="155"
						className={styles.projectionLine}
					/>

					{/* Punto de mercado actual */}
					<circle
						cx={currentX}
						cy={currentY}
						r="9"
						className={styles.pointPulse}
					/>
					<circle
						cx={currentX}
						cy={currentY}
						r="5"
						className={styles.pointActive}
					/>
				</svg>

				<div className={styles.footerNote}>
					<span>
						<strong>Lectura actual:</strong> Con {clampedBuy}% de demanda
						compradora, el multiplicador de mercado aplica una presión{" "}
						{bias === "BULLISH"
							? "alcista"
							: bias === "BEARISH"
								? "bajista"
								: "neutral"}{" "}
						en la cotización de los <TokenSymbol size="xs" /> cotizados.
					</span>
				</div>
			</div>
		</div>
	);
}
