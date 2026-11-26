import { useId, useMemo, useState } from "react";
import type { TokenQuoteSeries } from "@/features/players/types";
import { CREDIT_SYMBOL } from "@/shared/constants";
import styles from "./PlayerQuoteEvolutionChart.module.scss";

interface Props {
	series?: TokenQuoteSeries;
	className?: string;
}

export type TimeRange = "7D" | "1M" | "3M" | "1Y" | "ALL";

const WIDTH = 550;
const HEIGHT = 220;
const PAD_LEFT = 52;
const PAD_RIGHT = 16;
const PAD_TOP = 20;
const PAD_BOTTOM = 38;
const PLOT_WIDTH = WIDTH - PAD_LEFT - PAD_RIGHT;
const PLOT_HEIGHT = HEIGHT - PAD_TOP - PAD_BOTTOM;
const X_TICKS_DESKTOP = 5;

function formatDayMonth(value: string): string {
	if (!value) return "—";
	return `${value.slice(8, 10)}/${value.slice(5, 7)}`;
}

/** Calcula una escala agradable y limpia con múltiplos legibles (5, 10, 20, 50, etc.). */
function computeYScale(min: number, max: number, targetTicks = 4) {
	const rawRange = Math.max(max - min, 1);
	const rawStep = rawRange / targetTicks;
	const power = 10 ** Math.floor(Math.log10(rawStep || 1));
	const fraction = rawStep / power;
	let step: number;
	if (fraction <= 1) step = 1 * power;
	else if (fraction <= 2) step = 2 * power;
	else if (fraction <= 5) step = 5 * power;
	else step = 10 * power;

	step = Math.max(step, 5); // Mínimo salto de 5

	const minY = Math.max(0, Math.floor(min / step) * step);
	const maxY = Math.max(minY + step, Math.ceil(max / step) * step);
	const yValues: number[] = [];
	for (let val = minY; val <= maxY + step * 0.001; val += step) {
		yValues.push(Math.round(val));
	}
	return { minY, maxY: yValues[yValues.length - 1] ?? maxY, yValues };
}

/** Genera una curva Bezier cúbica suave estilo Catmull-Rom para trazado financiero pro. */
function createSmoothPath(coords: { x: number; y: number }[]): string {
	if (coords.length === 0) return "";
	if (coords.length === 1) return `M ${coords[0].x} ${coords[0].y}`;
	if (coords.length === 2) {
		return `M ${coords[0].x} ${coords[0].y} L ${coords[1].x} ${coords[1].y}`;
	}

	let d = `M ${coords[0].x.toFixed(1)} ${coords[0].y.toFixed(1)}`;
	for (let i = 0; i < coords.length - 1; i++) {
		const curr = coords[i];
		const next = coords[i + 1];
		const prev = coords[i - 1] ?? curr;
		const nextNext = coords[i + 2] ?? next;

		const cp1x = curr.x + (next.x - prev.x) * 0.16;
		const cp1y = curr.y + (next.y - prev.y) * 0.16;
		const cp2x = next.x - (nextNext.x - curr.x) * 0.16;
		const cp2y = next.y - (nextNext.y - curr.y) * 0.16;

		d += ` C ${cp1x.toFixed(1)} ${cp1y.toFixed(1)}, ${cp2x.toFixed(1)} ${cp2y.toFixed(1)}, ${next.x.toFixed(1)} ${next.y.toFixed(1)}`;
	}
	return d;
}

/**
 * Evolución interactiva de las cotizaciones oficiales del token en SVG propio,
 * con suavizado de curvas Bezier, cursor crosshair, tooltip dinámico y escala sin solapamientos.
 */
export default function PlayerQuoteEvolutionChart({
	series,
	className,
}: Props) {
	const gradientId = useId();
	const filterId = useId();
	const rawPoints = series?.points ?? [];

	// Orden cronológico asegurado: del más antiguo (izquierda) al más reciente (derecha).
	const sortedPoints = useMemo(() => {
		return [...rawPoints].sort(
			(a, b) => new Date(a.date).getTime() - new Date(b.date).getTime(),
		);
	}, [rawPoints]);

	const [selectedRange, setSelectedRange] = useState<TimeRange>("1M");
	const [hoverIndex, setHoverIndex] = useState<number | null>(null);

	const filteredPoints = useMemo(() => {
		if (sortedPoints.length === 0) return [];
		if (selectedRange === "ALL") return sortedPoints;

		const lastDate = new Date(sortedPoints[sortedPoints.length - 1].date);
		const daysMap: Record<TimeRange, number> = {
			"7D": 7,
			"1M": 30,
			"3M": 90,
			"1Y": 365,
			ALL: Number.POSITIVE_INFINITY,
		};
		const cutoff = new Date(lastDate);
		cutoff.setDate(cutoff.getDate() - daysMap[selectedRange]);

		const filtered = sortedPoints.filter((p) => new Date(p.date) >= cutoff);
		return filtered.length >= 2
			? filtered
			: sortedPoints.slice(-Math.min(sortedPoints.length, 7));
	}, [sortedPoints, selectedRange]);

	if (filteredPoints.length === 0)
		return (
			<section
				className={`${styles.playerQuoteChart} ${className ?? ""}`.trim()}
				aria-label="Evolución de la cotización"
			>
				<h2>Evolución de la cotización</h2>
				<p role="status">Aún no hay historial oficial de cotizaciones.</p>
			</section>
		);

	const prices = filteredPoints.map((point) => point.price);
	const min = Math.min(...prices);
	const max = Math.max(...prices);
	const firstPrice = filteredPoints[0]?.price ?? 0;
	const lastPrice = filteredPoints[filteredPoints.length - 1]?.price ?? 0;
	const netChange = lastPrice - firstPrice;
	const netPercentage = firstPrice > 0 ? (netChange / firstPrice) * 100 : 0;

	// Escala Y limpia y sin números extraños
	const { minY, maxY, yValues } = computeYScale(min, max);
	const spanY = maxY - minY || 1;

	const baselineY = PAD_TOP + PLOT_HEIGHT;

	const coordinates = filteredPoints.map(
		(point: TokenQuoteSeries["points"][0], index: number) => {
			const x =
				filteredPoints.length > 1
					? PAD_LEFT + (PLOT_WIDTH / (filteredPoints.length - 1)) * index
					: PAD_LEFT + PLOT_WIDTH / 2;
			const y =
				PAD_TOP + PLOT_HEIGHT - ((point.price - minY) / spanY) * PLOT_HEIGHT;

			return { x, y, point };
		},
	);

	const linePath = createSmoothPath(coordinates);
	const areaPath = `${linePath} L ${coordinates[coordinates.length - 1].x.toFixed(1)} ${baselineY} L ${coordinates[0].x.toFixed(1)} ${baselineY} Z`;

	// Marcas del eje X
	const tickCount = Math.min(X_TICKS_DESKTOP, filteredPoints.length);
	const tickIndexes = new Set<number>();
	for (let index = 0; index < tickCount; index += 1) {
		tickIndexes.add(
			Math.round((index * (filteredPoints.length - 1)) / (tickCount - 1 || 1)),
		);
	}

	// Puntos visibles sutiles
	const markerStride = Math.max(Math.round(filteredPoints.length / 7), 1);
	const markers = coordinates.filter(
		(
			_coord: { x: number; y: number; point: TokenQuoteSeries["points"][0] },
			index: number,
		) => index % markerStride === 0 || index === coordinates.length - 1,
	);
	const last = coordinates.at(-1);

	// Interacción hover
	const handlePointerMove = (e: React.PointerEvent<SVGSVGElement>) => {
		const rect = e.currentTarget.getBoundingClientRect();
		const clientX = e.clientX - rect.left;
		const svgX = (clientX / rect.width) * WIDTH;

		let closestIdx = 0;
		let minDiff = Number.POSITIVE_INFINITY;
		for (let i = 0; i < coordinates.length; i += 1) {
			const diff = Math.abs(coordinates[i].x - svgX);
			if (diff < minDiff) {
				minDiff = diff;
				closestIdx = i;
			}
		}
		setHoverIndex(closestIdx);
	};

	const handlePointerLeave = () => {
		setHoverIndex(null);
	};

	const activeCoord = hoverIndex !== null ? coordinates[hoverIndex] : null;
	const activePrice = activeCoord ? activeCoord.point.price : lastPrice;
	const activeChange =
		activeCoord && firstPrice > 0
			? ((activePrice - firstPrice) / firstPrice) * 100
			: netPercentage;

	// Posición del tooltip interactivo
	const tooltipWidth = 115;
	const tooltipHeight = 50;
	const tooltipX = activeCoord
		? activeCoord.x > WIDTH - 130
			? activeCoord.x - tooltipWidth - 10
			: activeCoord.x + 10
		: 0;
	const tooltipY = activeCoord
		? Math.max(
				PAD_TOP + 2,
				Math.min(
					activeCoord.y - tooltipHeight / 2,
					baselineY - tooltipHeight - 4,
				),
			)
		: 0;

	return (
		<section
			className={`${styles.playerQuoteChart} ${className ?? ""}`.trim()}
			aria-label="Evolución interactiva de la cotización"
		>
			<header className={styles.playerQuoteChart__header}>
				<div>
					<h2>Evolución de la cotización</h2>
					<span className={styles.playerQuoteChart__subtitle}>
						Cotización oficial persistida ({CREDIT_SYMBOL})
					</span>
				</div>

				<fieldset
					className={styles.playerQuoteChart__rangeSelector}
					aria-label="Seleccionar rango de tiempo"
				>
					{(["7D", "1M", "3M", "1Y", "ALL"] as const).map((range) => (
						<button
							key={range}
							type="button"
							className={
								selectedRange === range
									? styles.playerQuoteChart__rangeBtn_active
									: styles.playerQuoteChart__rangeBtn
							}
							aria-pressed={selectedRange === range}
							onClick={() => {
								setSelectedRange(range);
								setHoverIndex(null);
							}}
						>
							{range === "ALL" ? "Todo" : range}
						</button>
					))}
				</fieldset>
			</header>

			<div className={styles.playerQuoteChart__ribbon}>
				<div className={styles.playerQuoteChart__ribbonItem}>
					<span>
						Mínimo ({selectedRange === "ALL" ? "Todo" : selectedRange})
					</span>
					<strong>
						{min.toFixed(2)} {CREDIT_SYMBOL}
					</strong>
				</div>
				<div className={styles.playerQuoteChart__ribbonItem}>
					<span>
						Máximo ({selectedRange === "ALL" ? "Todo" : selectedRange})
					</span>
					<strong>
						{max.toFixed(2)} {CREDIT_SYMBOL}
					</strong>
				</div>
				<div className={styles.playerQuoteChart__ribbonItem}>
					<span>
						{activeCoord
							? `Sesión ${formatDayMonth(activeCoord.point.date)}`
							: "Variación periodo"}
					</span>
					<strong
						className={
							activeChange > 0
								? styles.playerQuoteChart__positive
								: activeChange < 0
									? styles.playerQuoteChart__negative
									: undefined
						}
					>
						{activePrice.toFixed(2)} {CREDIT_SYMBOL} (
						{activeChange > 0 ? "+" : ""}
						{activeChange.toFixed(2)} %)
					</strong>
				</div>
			</div>

			<div className={styles.playerQuoteChart__svgWrapper}>
				<svg
					className={styles.playerQuoteChart__svg}
					viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
					preserveAspectRatio="xMidYMid meet"
					role="img"
					focusable="false"
					aria-label={`Serie de ${filteredPoints.length} cotizaciones. Mínimo ${min} y máximo ${max}`}
					onPointerMove={handlePointerMove}
					onPointerLeave={handlePointerLeave}
				>
					<defs>
						<linearGradient
							id={`${gradientId}-line`}
							x1="0"
							y1="0"
							x2="1"
							y2="0"
						>
							<stop offset="0%" stopColor="#38bdf8" />
							<stop offset="100%" stopColor="#00dfa0" />
						</linearGradient>
						<linearGradient
							id={`${gradientId}-area`}
							x1="0"
							y1="0"
							x2="0"
							y2="1"
						>
							<stop offset="0%" stopColor="#38bdf8" stopOpacity="0.28" />
							<stop offset="100%" stopColor="#38bdf8" stopOpacity="0.0" />
						</linearGradient>
						<filter
							id={`${filterId}-shadow`}
							x="-20%"
							y="-20%"
							width="140%"
							height="140%"
						>
							<feDropShadow
								dx="0"
								dy="3"
								stdDeviation="4"
								floodColor="#000000"
								floodOpacity="0.5"
							/>
						</filter>
					</defs>

					{/* Símbolo de moneda sobre el eje Y (sin la palabra "Créditos") */}
					<text
						className={styles.playerQuoteChart__axisSymbol}
						x={PAD_LEFT - 8}
						y={13}
						textAnchor="end"
					>
						{CREDIT_SYMBOL}
					</text>

					{/* Líneas de cuadrícula Y */}
					{yValues.map((value) => {
						const y =
							PAD_TOP + PLOT_HEIGHT - ((value - minY) / spanY) * PLOT_HEIGHT;

						return (
							<g key={value}>
								<line
									className={styles.playerQuoteChart__gridLine}
									x1={PAD_LEFT}
									y1={y}
									x2={WIDTH - PAD_RIGHT}
									y2={y}
									strokeWidth={1}
									strokeDasharray="3 4"
								/>
								<text
									className={styles.playerQuoteChart__axisLabel}
									x={PAD_LEFT - 8}
									y={y + 3}
									textAnchor="end"
								>
									{value}
								</text>
							</g>
						);
					})}

					{/* Línea horizontal base */}
					<line
						className={styles.playerQuoteChart__axisLine}
						x1={PAD_LEFT}
						y1={baselineY}
						x2={WIDTH - PAD_RIGHT}
						y2={baselineY}
						strokeWidth={1}
					/>

					{/* Marcas de fechas del eje X */}
					{[...tickIndexes].map((index) => {
						const coordinate = coordinates[index];
						if (!coordinate) return null;

						return (
							<text
								key={coordinate.point.date}
								className={styles.playerQuoteChart__axisLabel}
								x={coordinate.x}
								y={baselineY + 16}
								textAnchor={
									index === 0
										? "start"
										: index === coordinates.length - 1
											? "end"
											: "middle"
								}
							>
								{formatDayMonth(coordinate.point.date)}
							</text>
						);
					})}

					{/* Área translúcida */}
					<path d={areaPath} fill={`url(#${gradientId}-area)`} />

					{/* Línea Bezier suave */}
					<path
						d={linePath}
						fill="none"
						stroke={`url(#${gradientId}-line)`}
						strokeWidth={2.5}
						strokeLinecap="round"
						strokeLinejoin="round"
					/>

					{/* Puntos guía sutiles */}
					{markers.map(({ x, y, point }) => (
						<circle
							className={styles.playerQuoteChart__marker}
							key={point.date}
							cx={x}
							cy={y}
							r={2.5}
							strokeWidth={1}
						/>
					))}

					{/* Último punto persistido */}
					{last && !activeCoord ? (
						<circle
							className={styles.playerQuoteChart__lastMarker}
							cx={last.x}
							cy={last.y}
							r={4.5}
							strokeWidth={1.5}
						/>
					) : null}

					{/* ELEMENTOS DE INTERACTIVIDAD (HOVER CROSSHAIR Y TOOLTIP) */}
					{activeCoord && (
						<g>
							{/* Línea vertical guía (crosshair) */}
							<line
								x1={activeCoord.x}
								y1={PAD_TOP}
								x2={activeCoord.x}
								y2={baselineY}
								className={styles.playerQuoteChart__crosshair}
							/>

							{/* Halo y punto activo */}
							<circle
								cx={activeCoord.x}
								cy={activeCoord.y}
								r={8}
								className={styles.playerQuoteChart__activeHalo}
							/>
							<circle
								cx={activeCoord.x}
								cy={activeCoord.y}
								r={4.5}
								className={styles.playerQuoteChart__activeDot}
							/>

							{/* Tooltip flotante interactivo */}
							<g
								className={styles.playerQuoteChart__tooltip}
								filter={`url(#${filterId}-shadow)`}
							>
								<rect
									x={tooltipX}
									y={tooltipY}
									width={tooltipWidth}
									height={tooltipHeight}
									rx={8}
									className={styles.playerQuoteChart__tooltipBg}
								/>
								<text
									x={tooltipX + 10}
									y={tooltipY + 16}
									className={styles.playerQuoteChart__tooltipDate}
								>
									{formatDayMonth(activeCoord.point.date)} • Sesión
								</text>
								<text
									x={tooltipX + 10}
									y={tooltipY + 32}
									className={styles.playerQuoteChart__tooltipPrice}
								>
									{activeCoord.point.price.toFixed(2)} {CREDIT_SYMBOL}
								</text>
								<text
									x={tooltipX + 10}
									y={tooltipY + 44}
									className={
										activeChange >= 0
											? styles.playerQuoteChart__tooltipGain
											: styles.playerQuoteChart__tooltipLoss
									}
								>
									{activeChange >= 0 ? "▲ +" : "▼ "}
									{activeChange.toFixed(2)} %
								</text>
							</g>
						</g>
					)}
				</svg>
			</div>
		</section>
	);
}
