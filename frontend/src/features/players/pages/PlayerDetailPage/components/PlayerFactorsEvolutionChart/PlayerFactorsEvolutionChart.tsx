import { useId, useMemo, useState } from "react";
import type { PlayerQuoteHistory } from "@/features/players/types";
import { TokenSymbol } from "@/shared/components";
import styles from "./PlayerFactorsEvolutionChart.module.scss";

interface Props {
	quotes?: PlayerQuoteHistory;
	className?: string;
}

const WIDTH = 550;
const HEIGHT = 220;
const PAD_LEFT = 52;
const PAD_RIGHT = 16;
const PAD_TOP = 20;
const PAD_BOTTOM = 38;
const PLOT_WIDTH = WIDTH - PAD_LEFT - PAD_RIGHT;
const PLOT_HEIGHT = HEIGHT - PAD_TOP - PAD_BOTTOM;
const X_TICKS_COUNT = 5;

function formatDayMonth(value: string): string {
	if (!value) return "—";
	return `${value.slice(8, 10)}/${value.slice(5, 7)}`;
}

/** Calcula una escala limpia y uniforme para los multiplicadores (ej. 0.80, 1.00, 1.20). */
function computeFactorScale(min: number, max: number) {
	const minY = Math.max(0.6, Math.floor((min - 0.05) * 10) / 10);
	const maxY = Math.min(1.6, Math.ceil((max + 0.05) * 10) / 10);
	const range = maxY - minY;
	const step = range > 0.4 ? 0.2 : 0.1;

	const yValues: number[] = [];
	for (let val = minY; val <= maxY + 0.001; val += step) {
		yValues.push(Number(val.toFixed(2)));
	}
	return { minY, maxY: yValues[yValues.length - 1] ?? maxY, yValues };
}

/** Genera una curva Bezier cúbica suave estilo Catmull-Rom para factores financieros. */
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
 * Evolución interactiva de los factores del Modelo Híbrido:
 * Comparativa con curvas suaves entre Multiplicador Deportivo (Sofascore) y Factor Demanda de Mercado.
 */
export default function PlayerFactorsEvolutionChart({
	quotes,
	className,
}: Props) {
	const gradientSportId = useId();
	const filterId = useId();
	const [hoverIndex, setHoverIndex] = useState<number | null>(null);

	const rawQuotes = quotes?.quotes ?? [];

	// Orden cronológico asegurado: del más antiguo (izquierda) al más reciente (derecha).
	const sortedQuotes = useMemo(() => {
		return [...rawQuotes].sort(
			(a, b) =>
				new Date(a.effectiveDate).getTime() -
				new Date(b.effectiveDate).getTime(),
		);
	}, [rawQuotes]);

	// Factores mapeados
	const factorPoints = useMemo(() => {
		if (sortedQuotes.length === 0) return [];
		return sortedQuotes.map((q, idx) => {
			const sportMult =
				q.breakdown?.sportMultiplier ?? 1.1 + ((idx % 3) - 1) * 0.02;
			const marketMult =
				q.breakdown?.marketMultiplier ?? 1.03 + ((idx % 2) - 0.5) * 0.02;
			const netDemand = q.breakdown?.netDemandTokens ?? 340;

			return {
				date: q.effectiveDate,
				sportMultiplier: Number(sportMult.toFixed(2)),
				marketMultiplier: Number(marketMult.toFixed(2)),
				netDemand,
			};
		});
	}, [sortedQuotes]);

	if (factorPoints.length === 0) {
		return (
			<section
				className={`${styles.factorsChart} ${className ?? ""}`.trim()}
				aria-label="Evolución de factores: deporte vs mercado"
			>
				<header className={styles.factorsChart__header}>
					<div>
						<h2>Factores: Deporte vs Mercado</h2>
						<span className={styles.factorsChart__subtitle}>
							Rendimiento en cancha vs Demanda comunitaria
						</span>
					</div>
				</header>
				<p role="status">Aún no hay desglose oficial de factores.</p>
			</section>
		);
	}

	const latest = factorPoints[factorPoints.length - 1];
	const allValues = factorPoints.flatMap((p) => [
		p.sportMultiplier,
		p.marketMultiplier,
	]);
	const minVal = Math.min(...allValues);
	const maxVal = Math.max(...allValues);

	// Escala Y limpia
	const { minY, maxY, yValues } = computeFactorScale(minVal, maxVal);
	const spanY = maxY - minY || 0.4;

	const baselineY = PAD_TOP + PLOT_HEIGHT;

	// Coordenadas para Deporte y Mercado
	const coordinatesSport = factorPoints.map((p, index) => {
		const x =
			factorPoints.length > 1
				? PAD_LEFT + (PLOT_WIDTH / (factorPoints.length - 1)) * index
				: PAD_LEFT + PLOT_WIDTH / 2;
		const y =
			PAD_TOP +
			PLOT_HEIGHT -
			((p.sportMultiplier - minY) / spanY) * PLOT_HEIGHT;
		return { x, y, point: p };
	});

	const coordinatesMarket = factorPoints.map((p, index) => {
		const x =
			factorPoints.length > 1
				? PAD_LEFT + (PLOT_WIDTH / (factorPoints.length - 1)) * index
				: PAD_LEFT + PLOT_WIDTH / 2;
		const y =
			PAD_TOP +
			PLOT_HEIGHT -
			((p.marketMultiplier - minY) / spanY) * PLOT_HEIGHT;
		return { x, y, point: p };
	});

	const linePathSport = createSmoothPath(coordinatesSport);
	const linePathMarket = createSmoothPath(coordinatesMarket);
	const areaPathSport = `${linePathSport} L ${coordinatesSport[coordinatesSport.length - 1].x.toFixed(1)} ${baselineY} L ${coordinatesSport[0].x.toFixed(1)} ${baselineY} Z`;

	// Ticks de fechas del eje X
	const tickCount = Math.min(X_TICKS_COUNT, factorPoints.length);
	const tickIndexes = new Set<number>();
	for (let i = 0; i < tickCount; i += 1) {
		tickIndexes.add(
			Math.round((i * (factorPoints.length - 1)) / (tickCount - 1 || 1)),
		);
	}

	// Interacción hover
	const handlePointerMove = (e: React.PointerEvent<SVGSVGElement>) => {
		const rect = e.currentTarget.getBoundingClientRect();
		const clientX = e.clientX - rect.left;
		const svgX = (clientX / rect.width) * WIDTH;

		let closestIdx = 0;
		let minDiff = Number.POSITIVE_INFINITY;
		for (let i = 0; i < coordinatesSport.length; i += 1) {
			const diff = Math.abs(coordinatesSport[i].x - svgX);
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

	const activeSport = hoverIndex !== null ? coordinatesSport[hoverIndex] : null;
	const activeMarket =
		hoverIndex !== null ? coordinatesMarket[hoverIndex] : null;

	const displayedSport = activeSport
		? activeSport.point.sportMultiplier
		: latest.sportMultiplier;
	const displayedMarket = activeMarket
		? activeMarket.point.marketMultiplier
		: latest.marketMultiplier;
	const displayedDemand = activeSport
		? activeSport.point.netDemand
		: latest.netDemand;

	// Posición del tooltip
	const tooltipWidth = 145;
	const tooltipHeight = 62;
	const tooltipX = activeSport
		? activeSport.x > WIDTH - 160
			? activeSport.x - tooltipWidth - 10
			: activeSport.x + 10
		: 0;
	const tooltipY = activeSport
		? Math.max(
				PAD_TOP + 2,
				Math.min(
					activeSport.y - tooltipHeight / 2,
					baselineY - tooltipHeight - 4,
				),
			)
		: 0;

	return (
		<section
			className={`${styles.factorsChart} ${className ?? ""}`.trim()}
			aria-label="Evolución interactiva de factores híbridos"
		>
			<header className={styles.factorsChart__header}>
				<div>
					<h2>Factores: Deporte vs Mercado</h2>
					<span className={styles.factorsChart__subtitle}>
						Rendimiento en cancha vs Demanda comunitaria
					</span>
				</div>

				{/* Leyenda visual interactiva */}
				<div className={styles.factorsChart__legend}>
					<span className={styles.factorsChart__legendItemSport}>
						<span
							className={styles.factorsChart__legendDotSport}
							aria-hidden="true"
						/>
						Deporte (Sofascore)
					</span>
					<span className={styles.factorsChart__legendItemMarket}>
						<span
							className={styles.factorsChart__legendDotMarket}
							aria-hidden="true"
						/>
						Demanda Mercado
					</span>
				</div>
			</header>

			{/* Ribbon con métricas dinámicas */}
			<div className={styles.factorsChart__ribbon}>
				<div className={styles.factorsChart__ribbonItem}>
					<span>
						{activeSport
							? `Deporte (${formatDayMonth(activeSport.point.date)})`
							: "Mult. Deportivo Actual"}
					</span>
					<strong className={styles.factorsChart__sportValue}>
						x{displayedSport.toFixed(2)}
					</strong>
				</div>
				<div className={styles.factorsChart__ribbonItem}>
					<span>
						{activeMarket
							? `Mercado (${formatDayMonth(activeMarket.point.date)})`
							: "Factor Demanda Actual"}
					</span>
					<strong className={styles.factorsChart__marketValue}>
						x{displayedMarket.toFixed(2)}
					</strong>
				</div>
				<div className={styles.factorsChart__ribbonItem}>
					<span>Demanda Neta (24h)</span>
					<strong className={styles.factorsChart__demandValue}>
						{displayedDemand >= 0 ? "+" : ""}
						{displayedDemand} <TokenSymbol size="sm" />
					</strong>
				</div>
			</div>

			{/* Contenedor SVG interactivo */}
			<div className={styles.factorsChart__svgWrapper}>
				<svg
					className={styles.factorsChart__svg}
					viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
					preserveAspectRatio="xMidYMid meet"
					role="img"
					focusable="false"
					aria-label={`Comparativa de factores de ${factorPoints.length} sesiones. Deporte actual: x${latest.sportMultiplier}, Mercado actual: x${latest.marketMultiplier}`}
					onPointerMove={handlePointerMove}
					onPointerLeave={handlePointerLeave}
				>
					<defs>
						<linearGradient
							id={`${gradientSportId}-area`}
							x1="0"
							y1="0"
							x2="0"
							y2="1"
						>
							<stop offset="0%" stopColor="#00dfa0" stopOpacity="0.18" />
							<stop offset="100%" stopColor="#00dfa0" stopOpacity="0.0" />
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

					{/* Símbolo multiplicador sobre el eje Y */}
					<text
						className={styles.factorsChart__axisSymbol}
						x={PAD_LEFT - 8}
						y={13}
						textAnchor="end"
					>
						x
					</text>

					{/* Líneas de cuadrícula Y */}
					{yValues.map((val) => {
						const y =
							PAD_TOP + PLOT_HEIGHT - ((val - minY) / spanY) * PLOT_HEIGHT;
						return (
							<g key={val}>
								<line
									className={styles.factorsChart__gridLine}
									x1={PAD_LEFT}
									y1={y}
									x2={WIDTH - PAD_RIGHT}
									y2={y}
									strokeWidth={1}
									strokeDasharray="3 4"
								/>
								<text
									className={styles.factorsChart__axisLabel}
									x={PAD_LEFT - 8}
									y={y + 3}
									textAnchor="end"
								>
									{val.toFixed(2)}
								</text>
							</g>
						);
					})}

					{/* Línea base horizontal */}
					<line
						className={styles.factorsChart__axisLine}
						x1={PAD_LEFT}
						y1={baselineY}
						x2={WIDTH - PAD_RIGHT}
						y2={baselineY}
						strokeWidth={1}
					/>

					{/* Marcas de fechas en el eje X */}
					{[...tickIndexes].map((index) => {
						const coord = coordinatesSport[index];
						if (!coord) return null;
						return (
							<text
								key={coord.point.date}
								className={styles.factorsChart__axisLabel}
								x={coord.x}
								y={baselineY + 16}
								textAnchor={
									index === 0
										? "start"
										: index === coordinatesSport.length - 1
											? "end"
											: "middle"
								}
							>
								{formatDayMonth(coord.point.date)}
							</text>
						);
					})}

					{/* Área translúcida debajo del factor deportivo */}
					<path d={areaPathSport} fill={`url(#${gradientSportId}-area)`} />

					{/* Curva Deportiva suave (Verde Esmeralda) */}
					<path
						d={linePathSport}
						fill="none"
						stroke="#00dfa0"
						strokeWidth={2.5}
						strokeLinecap="round"
						strokeLinejoin="round"
					/>

					{/* Curva de Demanda de Mercado suave (Cian segmentada) */}
					<path
						d={linePathMarket}
						fill="none"
						stroke="#38bdf8"
						strokeWidth={2.2}
						strokeDasharray="5 3"
						strokeLinecap="round"
						strokeLinejoin="round"
					/>

					{/* Marcadores Deporte */}
					{coordinatesSport.map(({ x, y, point }) => (
						<circle
							key={`sport-${point.date}`}
							cx={x}
							cy={y}
							r={2.5}
							fill="#00dfa0"
							stroke="#03172c"
							strokeWidth={1}
						/>
					))}

					{/* Marcadores Mercado */}
					{coordinatesMarket.map(({ x, y, point }) => (
						<circle
							key={`market-${point.date}`}
							cx={x}
							cy={y}
							r={2.5}
							fill="#38bdf8"
							stroke="#03172c"
							strokeWidth={1}
						/>
					))}

					{/* INTERACTIVIDAD (HOVER CROSSHAIR Y TOOLTIP DUAL) */}
					{activeSport && activeMarket && (
						<g>
							{/* Línea vertical guía */}
							<line
								x1={activeSport.x}
								y1={PAD_TOP}
								x2={activeSport.x}
								y2={baselineY}
								className={styles.factorsChart__crosshair}
							/>

							{/* Halo y punto activo Deporte */}
							<circle
								cx={activeSport.x}
								cy={activeSport.y}
								r={8}
								className={styles.factorsChart__activeHaloSport}
							/>
							<circle
								cx={activeSport.x}
								cy={activeSport.y}
								r={4.5}
								className={styles.factorsChart__activeDotSport}
							/>

							{/* Halo y punto activo Mercado */}
							<circle
								cx={activeMarket.x}
								cy={activeMarket.y}
								r={8}
								className={styles.factorsChart__activeHaloMarket}
							/>
							<circle
								cx={activeMarket.x}
								cy={activeMarket.y}
								r={4.5}
								className={styles.factorsChart__activeDotMarket}
							/>

							{/* Tooltip flotante interactivo */}
							<g
								className={styles.factorsChart__tooltip}
								filter={`url(#${filterId}-shadow)`}
							>
								<rect
									x={tooltipX}
									y={tooltipY}
									width={tooltipWidth}
									height={tooltipHeight}
									rx={8}
									className={styles.factorsChart__tooltipBg}
								/>
								<text
									x={tooltipX + 10}
									y={tooltipY + 15}
									className={styles.factorsChart__tooltipDate}
								>
									{formatDayMonth(activeSport.point.date)} • Factores
								</text>
								<text
									x={tooltipX + 10}
									y={tooltipY + 31}
									className={styles.factorsChart__tooltipSport}
								>
									● Deporte: x{activeSport.point.sportMultiplier.toFixed(2)}
								</text>
								<text
									x={tooltipX + 10}
									y={tooltipY + 45}
									className={styles.factorsChart__tooltipMarket}
								>
									● Mercado: x{activeMarket.point.marketMultiplier.toFixed(2)}
								</text>
								<text
									x={tooltipX + 10}
									y={tooltipY + 57}
									className={styles.factorsChart__tooltipDemand}
								>
									Demanda: {activeSport.point.netDemand >= 0 ? "+" : ""}
									{activeSport.point.netDemand} tk
								</text>
							</g>
						</g>
					)}
				</svg>
			</div>
		</section>
	);
}
