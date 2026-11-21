import { useId, useMemo, useState } from "react";
import { TrendingUp } from "@/shared/icons";
import type { PerformanceSeries } from "../../types/models";
import styles from "./PerformanceBenchmarkChart.module.scss";

export interface PerformanceBenchmarkChartProps {
	data?: PerformanceSeries;
	activeRange: "1M" | "3M" | "ALL";
	onRangeChange: (range: "1M" | "3M" | "ALL") => void;
	isLoading?: boolean;
	className?: string;
}

const WIDTH = 640;
const HEIGHT = 240;
const PAD_LEFT = 50;
const PAD_RIGHT = 20;
const PAD_TOP = 20;
const PAD_BOTTOM = 30;
const PLOT_WIDTH = WIDTH - PAD_LEFT - PAD_RIGHT;
const PLOT_HEIGHT = HEIGHT - PAD_TOP - PAD_BOTTOM;

export function PerformanceBenchmarkChart({
	data,
	activeRange,
	onRangeChange,
	isLoading,
	className,
}: PerformanceBenchmarkChartProps) {
	const [hoverIndex, setHoverIndex] = useState<number | null>(null);
	const userGradientId = useId();
	const benchmarkGradientId = useId();

	const dates = data?.dates ?? [];
	const userSeries = data?.userPortfolioSeries ?? [];
	const benchmarkSeries = data?.marketBenchmarkSeries ?? [];

	const hasData = dates.length > 1;

	// Escala de valores
	const { minY, maxY, spanY } = useMemo(() => {
		if (!hasData) return { minY: 0, maxY: 100, spanY: 100 };
		const allValues = [...userSeries, ...benchmarkSeries];
		const min = Math.min(...allValues);
		const max = Math.max(...allValues);
		const padding = (max - min) * 0.1 || 10;
		const calculatedMin = Math.max(0, Math.floor((min - padding) / 10) * 10);
		const calculatedMax = Math.ceil((max + padding) / 10) * 10;
		return {
			minY: calculatedMin,
			maxY: calculatedMax,
			spanY: calculatedMax - calculatedMin || 1,
		};
	}, [hasData, userSeries, benchmarkSeries]);

	// Coordenadas
	const { userPoints, benchmarkPoints } = useMemo(() => {
		if (!hasData) return { userPoints: [], benchmarkPoints: [] };
		const count = dates.length;

		const toCoordinates = (series: number[]) =>
			series.map((val, idx) => {
				const x = PAD_LEFT + (PLOT_WIDTH / (count - 1)) * idx;
				const y = PAD_TOP + PLOT_HEIGHT - ((val - minY) / spanY) * PLOT_HEIGHT;
				return { x, y, val };
			});

		return {
			userPoints: toCoordinates(userSeries),
			benchmarkPoints: toCoordinates(benchmarkSeries),
		};
	}, [hasData, dates, userSeries, benchmarkSeries, minY, spanY]);

	// Path strings
	const userPath = useMemo(() => {
		if (userPoints.length === 0) return "";
		return userPoints.reduce(
			(acc, pt, idx) =>
				idx === 0 ? `M ${pt.x} ${pt.y}` : `${acc} L ${pt.x} ${pt.y}`,
			"",
		);
	}, [userPoints]);

	const userAreaPath = useMemo(() => {
		if (userPoints.length === 0) return "";
		const first = userPoints[0];
		const last = userPoints[userPoints.length - 1];
		return `${userPath} L ${last.x} ${PAD_TOP + PLOT_HEIGHT} L ${first.x} ${PAD_TOP + PLOT_HEIGHT} Z`;
	}, [userPath, userPoints]);

	const benchmarkPath = useMemo(() => {
		if (benchmarkPoints.length === 0) return "";
		return benchmarkPoints.reduce(
			(acc, pt, idx) =>
				idx === 0 ? `M ${pt.x} ${pt.y}` : `${acc} L ${pt.x} ${pt.y}`,
			"",
		);
	}, [benchmarkPoints]);

	const formatDayMonth = (dateStr: string) => {
		if (dateStr.length >= 10) {
			return `${dateStr.slice(8, 10)}/${dateStr.slice(5, 7)}`;
		}
		return dateStr;
	};

	return (
		<div className={`${styles.card} ${className ?? ""}`.trim()}>
			<header className={styles.header}>
				<div className={styles.headerLeft}>
					<span className={styles.headerIcon} aria-hidden="true">
						<TrendingUp />
					</span>
					<div className={styles.titleGroup}>
						<h2 className={styles.title}>Benchmark de Rendimiento</h2>
						<span className={styles.subtitle}>
							Evolución comparada vs Football Market Index (FMI)
						</span>
					</div>
				</div>

				<div className={styles.controls}>
					<div className={styles.legend}>
						<span className={styles.legendUser}>
							<span className={styles.legendDotUser} /> Mi Cartera
						</span>
						<span className={styles.legendBenchmark}>
							<span className={styles.legendDotBenchmark} /> Índice FMI
						</span>
					</div>

					<div className={styles.rangeButtons}>
						{(["1M", "3M", "ALL"] as const).map((range) => (
							<button
								key={range}
								type="button"
								className={`${styles.rangeBtn} ${activeRange === range ? styles.rangeBtnActive : ""}`}
								onClick={() => onRangeChange(range)}
								disabled={isLoading}
							>
								{range}
							</button>
						))}
					</div>
				</div>
			</header>

			<div className={styles.chartContainer}>
				{!hasData ? (
					<div className={styles.emptyContainer}>
						<span className={styles.emptyIcon} aria-hidden="true">
							📊
						</span>
						<p className={styles.emptyTitle}>
							Historial en proceso de consolidación
						</p>
						<p className={styles.emptyDesc}>
							La curva de rendimiento se genera automáticamente a medida que se
							acumulan sesiones de recálculo oficial del mercado.
						</p>
					</div>
				) : (
					<svg
						viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
						className={styles.svg}
						role="img"
						aria-label="Gráfico de benchmark de rendimiento comparado contra el índice FMI"
						onMouseLeave={() => setHoverIndex(null)}
						onMouseMove={(e) => {
							const rect = e.currentTarget.getBoundingClientRect();
							const clientX = e.clientX - rect.left;
							const relativeX = (clientX / rect.width) * WIDTH;
							if (relativeX < PAD_LEFT || relativeX > WIDTH - PAD_RIGHT) {
								setHoverIndex(null);
								return;
							}
							const ratio = (relativeX - PAD_LEFT) / PLOT_WIDTH;
							const idx = Math.round(ratio * (dates.length - 1));
							if (idx >= 0 && idx < dates.length) {
								setHoverIndex(idx);
							}
						}}
					>
						<title>Benchmark de rendimiento de cartera vs FMI</title>
						<defs>
							<linearGradient id={userGradientId} x1="0" y1="0" x2="0" y2="1">
								<stop offset="0%" className={styles.stopUserTop} />
								<stop offset="100%" className={styles.stopUserBottom} />
							</linearGradient>
							<linearGradient
								id={benchmarkGradientId}
								x1="0"
								y1="0"
								x2="0"
								y2="1"
							>
								<stop offset="0%" className={styles.stopBenchmarkTop} />
								<stop offset="100%" className={styles.stopBenchmarkBottom} />
							</linearGradient>
						</defs>

						{/* Líneas horizontales de cuadrícula */}
						{[0, 0.25, 0.5, 0.75, 1].map((pct) => {
							const y = PAD_TOP + PLOT_HEIGHT * pct;
							const val = Math.round(maxY - pct * (maxY - minY));
							return (
								<g key={pct} className={styles.gridGroup}>
									<line
										x1={PAD_LEFT}
										y1={y}
										x2={WIDTH - PAD_RIGHT}
										y2={y}
										className={styles.gridLine}
									/>
									<text
										x={PAD_LEFT - 8}
										y={y + 4}
										textAnchor="end"
										className={styles.axisLabel}
									>
										{val} ₵
									</text>
								</g>
							);
						})}

						{/* Líneas de fecha en el eje X */}
						{dates.map((d, idx) => {
							const step = Math.max(1, Math.floor(dates.length / 5));
							if (idx % step !== 0 && idx !== dates.length - 1) return null;
							const x = PAD_LEFT + (PLOT_WIDTH / (dates.length - 1)) * idx;
							return (
								<text
									key={d}
									x={x}
									y={HEIGHT - 8}
									textAnchor="middle"
									className={styles.axisLabel}
								>
									{formatDayMonth(d)}
								</text>
							);
						})}

						{/* Áreas de relleno */}
						<path
							d={userAreaPath}
							fill={`url(#${userGradientId})`}
							className={styles.userArea}
						/>

						{/* Línea FMI Benchmark */}
						<path
							d={benchmarkPath}
							fill="none"
							className={styles.benchmarkLine}
							strokeWidth="2"
							strokeDasharray="4,4"
						/>

						{/* Línea Cartera Usuario */}
						<path
							d={userPath}
							fill="none"
							className={styles.userLine}
							strokeWidth="2.5"
						/>

						{/* Indicador de tooltip activo */}
						{hoverIndex !== null && userPoints[hoverIndex] && (
							<g className={styles.crosshairGroup}>
								<line
									x1={userPoints[hoverIndex].x}
									y1={PAD_TOP}
									x2={userPoints[hoverIndex].x}
									y2={PAD_TOP + PLOT_HEIGHT}
									className={styles.crosshairLine}
								/>
								<circle
									cx={userPoints[hoverIndex].x}
									cy={userPoints[hoverIndex].y}
									r="4.5"
									className={styles.userPointActive}
								/>
								{benchmarkPoints[hoverIndex] && (
									<circle
										cx={benchmarkPoints[hoverIndex].x}
										cy={benchmarkPoints[hoverIndex].y}
										r="4"
										className={styles.benchmarkPointActive}
									/>
								)}
							</g>
						)}
					</svg>
				)}

				{hoverIndex !== null && dates[hoverIndex] && (
					<div
						className={styles.tooltipCard}
						style={{
							left: `${((userPoints[hoverIndex]?.x ?? PAD_LEFT) / WIDTH) * 100}%`,
						}}
					>
						<span className={styles.tooltipDate}>
							{formatDayMonth(dates[hoverIndex])}
						</span>
						<span className={styles.tooltipUser}>
							Mi Cartera: {userSeries[hoverIndex]?.toFixed(2)} ₵
						</span>
						<span className={styles.tooltipBenchmark}>
							FMI: {benchmarkSeries[hoverIndex]?.toFixed(2)} ₵
						</span>
					</div>
				)}
			</div>
		</div>
	);
}
