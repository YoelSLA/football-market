import { useId, useMemo, useState } from "react";
import type { PortfolioAllocation } from "../../types/models";
import styles from "./PortfolioAllocationChart.module.scss";
import { calculateDonutSegments } from "./utils/donutMath";

export interface PortfolioAllocationChartProps {
	allocation: PortfolioAllocation;
	className?: string;
}

export function PortfolioAllocationChart({
	allocation,
	className,
}: PortfolioAllocationChartProps) {
	const [dimension, setDimension] = useState<"position" | "league">("position");
	const [hoveredKey, setHoveredKey] = useState<string | null>(null);
	const chartId = useId();

	const activeItems =
		dimension === "position" ? allocation.byPosition : allocation.byLeague;

	const totalValue = useMemo(() => {
		return activeItems.reduce((acc, item) => acc + item.totalValue, 0);
	}, [activeItems]);

	const totalTokens = useMemo(() => {
		return activeItems.reduce((acc, item) => acc + item.tokenCount, 0);
	}, [activeItems]);

	const segments = useMemo(() => {
		return calculateDonutSegments(activeItems, 100, 100, 62, 90);
	}, [activeItems]);

	const activeSegment = useMemo(() => {
		if (!hoveredKey) return null;
		return segments.find((s) => s.key === hoveredKey) ?? null;
	}, [segments, hoveredKey]);

	const hasConcentrationRisk =
		allocation.topHoldingConcentrationPercentage > 40;

	return (
		<div className={`${styles.card} ${className ?? ""}`.trim()}>
			<header className={styles.header}>
				<div className={styles.titleGroup}>
					<h2 className={styles.title}>Distribución de Cartera</h2>
					<span className={styles.subtitle}>
						Asignación de capital por activo deportivo
					</span>
				</div>

				<div className={styles.controls}>
					<div className={styles.toggleGroup}>
						<button
							type="button"
							className={`${styles.toggleButton} ${dimension === "position" ? styles.toggleButtonActive : ""}`}
							onClick={() => {
								setDimension("position");
								setHoveredKey(null);
							}}
						>
							Posición
						</button>
						<button
							type="button"
							className={`${styles.toggleButton} ${dimension === "league" ? styles.toggleButtonActive : ""}`}
							onClick={() => {
								setDimension("league");
								setHoveredKey(null);
							}}
						>
							Liga
						</button>
					</div>
				</div>
			</header>

			{hasConcentrationRisk && (
				<div className={styles.concentrationAlert} role="alert">
					<span className={styles.alertIcon} aria-hidden="true">
						⚠️
					</span>
					<span className={styles.alertText}>
						<strong>Alerta de concentración:</strong>{" "}
						{allocation.topHoldingPlayerName} representa el{" "}
						{allocation.topHoldingConcentrationPercentage.toFixed(1)}% del valor
						total de la cartera.
					</span>
				</div>
			)}

			<div className={styles.body}>
				{activeItems.length === 0 ? (
					<div className={styles.emptyState}>
						<p>No hay activos registrados en cartera.</p>
					</div>
				) : (
					<>
						<div className={styles.chartWrapper}>
							<svg
								viewBox="0 0 200 200"
								className={styles.donutSvg}
								aria-labelledby={chartId}
							>
								<title id={chartId}>
									Gráfico de distribución por{" "}
									{dimension === "position" ? "posición" : "liga"}
								</title>
								{segments.map((seg) => {
									const isHovered = hoveredKey === seg.key;
									return (
										<path
											key={seg.key}
											d={seg.path}
											fill={seg.color}
											className={`${styles.segment} ${isHovered ? styles.segmentHovered : ""}`}
										/>
									);
								})}

								{/* Centro informativo del donut */}
								<g className={styles.centerText} pointerEvents="none">
									{activeSegment ? (
										<>
											<text
												x="100"
												y="92"
												textAnchor="middle"
												className={styles.centerLabel}
											>
												{activeSegment.label}
											</text>
											<text
												x="100"
												y="112"
												textAnchor="middle"
												className={styles.centerValue}
											>
												{activeSegment.percentage.toFixed(1)}%
											</text>
										</>
									) : (
										<>
											<text
												x="100"
												y="88"
												textAnchor="middle"
												className={styles.centerLabel}
											>
												Total Cartera
											</text>
											<text
												x="100"
												y="108"
												textAnchor="middle"
												className={styles.centerValue}
											>
												{totalValue.toLocaleString("es-ES", {
													maximumFractionDigits: 0,
												})}{" "}
												₢
											</text>
											<text
												x="100"
												y="124"
												textAnchor="middle"
												className={styles.centerSub}
											>
												{totalTokens} tokens
											</text>
										</>
									)}
								</g>
							</svg>
						</div>

						<ul
							className={styles.legendList}
							aria-label="Desglose por categoría"
						>
							{segments.map((seg) => {
								const isHovered = hoveredKey === seg.key;
								return (
									<li
										key={seg.key}
										className={`${styles.legendItem} ${isHovered ? styles.legendItemHovered : ""}`}
										onMouseEnter={() => setHoveredKey(seg.key)}
										onMouseLeave={() => setHoveredKey(null)}
									>
										<span
											className={styles.legendColor}
											style={{ backgroundColor: seg.color }}
											aria-hidden="true"
										/>
										<span className={styles.legendLabel}>{seg.label}</span>
										<span className={styles.legendPct}>
											{seg.percentage.toFixed(1)}%
										</span>
										<span className={styles.legendValue}>
											{seg.value.toLocaleString("es-ES", {
												minimumFractionDigits: 2,
												maximumFractionDigits: 2,
											})}{" "}
											₢
										</span>
									</li>
								);
							})}
						</ul>
					</>
				)}
			</div>
		</div>
	);
}
