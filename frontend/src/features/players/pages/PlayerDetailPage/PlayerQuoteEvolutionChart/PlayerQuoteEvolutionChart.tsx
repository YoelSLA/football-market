import { useId } from "react";
import {
	DEMO_DATA_LABEL,
	DEMO_MESSAGES,
	DEMO_QUOTE_UNIT_LABEL,
} from "@/features/players/constants";
import type { TokenQuoteSeries } from "@/features/players/types";
import styles from "./PlayerQuoteEvolutionChart.module.scss";

interface Props {
	series?: TokenQuoteSeries;
	className?: string;
}

const WIDTH = 720;
const HEIGHT = 280;
const PAD_LEFT = 58;
const PAD_RIGHT = 18;
const PAD_TOP = 16;
const PAD_BOTTOM = 34;
const PLOT_WIDTH = WIDTH - PAD_LEFT - PAD_RIGHT;
const PLOT_HEIGHT = HEIGHT - PAD_TOP - PAD_BOTTOM;
const Y_GRID_LINES = 4;
const X_TICKS_DESKTOP = 5;

/** Escala el rango a múltiplos de 10 para que las etiquetas caigan en valores legibles. */
function niceStep(value: number): number {
	const raw = Math.max(Math.ceil(value / Y_GRID_LINES), 1);

	return Math.max(Math.ceil(raw / 10) * 10, 10);
}

function formatDayMonth(value: string): string {
	return `${value.slice(8, 10)}/${value.slice(5, 7)}`;
}

/**
 * Evolución de la cotización de demostración en SVG propio, sin dependencia nueva.
 * Conserva los 30 valores de la serie, muestra ejes con fechas y créditos, y expone
 * todos los valores en texto y tabla para que la serie sea comprensible sin depender
 * del dibujo ni del color.
 */
export default function PlayerQuoteEvolutionChart({
	series,
	className,
}: Props) {
	const gradientId = useId();
	const points = series?.points ?? [];

	if (points.length === 0)
		return (
			<section
				className={`${styles.playerQuoteChart} ${className ?? ""}`.trim()}
				aria-label="Evolución de la cotización"
			>
				<h2>Evolución de la cotización</h2>
				<p role="status">{DEMO_MESSAGES.emptySeries}</p>
				<p className={styles.playerQuoteChart__demo}>{DEMO_DATA_LABEL}</p>
			</section>
		);

	const prices = points.map(
		(point: TokenQuoteSeries["points"][0]) => point.price,
	);
	const min = Math.min(...prices);
	const max = Math.max(...prices);
	const step = niceStep(Math.max(max - min, 1));
	const minY = Math.max(0, Math.floor((min - step) / 10) * 10);
	const maxY = Math.ceil((max + step) / 10) * 10;
	const spanY = maxY - minY || 1;

	const coordinates = points.map(
		(point: TokenQuoteSeries["points"][0], index: number) => {
			const x =
				points.length > 1
					? PAD_LEFT + (PLOT_WIDTH / (points.length - 1)) * index
					: PAD_LEFT + PLOT_WIDTH / 2;
			const y =
				PAD_TOP + PLOT_HEIGHT - ((point.price - minY) / spanY) * PLOT_HEIGHT;

			return { x, y, point };
		},
	);

	const linePath = coordinates
		.map(({ x, y }, index: number) => `${index === 0 ? "M" : "L"}${x} ${y}`)
		.join(" ");

	const baselineY = PAD_TOP + PLOT_HEIGHT;
	const areaPath = `${linePath} L${coordinates.at(-1)?.x ?? PAD_LEFT} ${baselineY} L${coordinates[0].x} ${baselineY} Z`;

	// Referencias del eje Y, sin etiquetas duplicadas por redondeo.
	const yValues: number[] = [];
	for (let index = 0; index <= Y_GRID_LINES; index += 1) {
		const value = minY + (spanY / Y_GRID_LINES) * index;
		const rounded = Math.round(value);
		if (!yValues.includes(rounded)) yValues.push(rounded);
	}

	// Marcas del eje X: evenly repartidas por índice, nunca por fecha interpolada.
	const tickCount = Math.min(X_TICKS_DESKTOP, points.length);
	const tickIndexes = new Set<number>();
	for (let index = 0; index < tickCount; index += 1) {
		tickIndexes.add(
			Math.round((index * (points.length - 1)) / (tickCount - 1 || 1)),
		);
	}

	// Puntos visibles reducidos para evitar saturación; la serie completa sigue en la tabla.
	const markerStride = Math.max(Math.round(points.length / 7), 1);
	const markers = coordinates.filter(
		(
			_coord: { x: number; y: number; point: TokenQuoteSeries["points"][0] },
			index: number,
		) => index % markerStride === 0 || index === coordinates.length - 1,
	);
	const last = coordinates.at(-1);

	return (
		<section
			className={`${styles.playerQuoteChart} ${className ?? ""}`.trim()}
			aria-label="Evolución de la cotización"
		>
			<header>
				<h2>Evolución de la cotización</h2>
				<span>{DEMO_QUOTE_UNIT_LABEL}</span>
			</header>

			<svg
				className={styles.playerQuoteChart__svg}
				viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
				preserveAspectRatio="xMidYMid meet"
				role="img"
				focusable="false"
				aria-label={`Serie de ${points.length} cotizaciones diarias en ${DEMO_QUOTE_UNIT_LABEL}, del ${points[0].date} al ${points.at(-1)?.date}, con precio mínimo ${min} y máximo ${max}`}
			>
				<defs>
					<linearGradient id={`${gradientId}-line`} x1="0" y1="0" x2="0" y2="1">
						<stop offset="0%" stopColor="#7dd3fc" />
						<stop offset="100%" stopColor="#38bdf8" />
					</linearGradient>
					<linearGradient id={`${gradientId}-area`} x1="0" y1="0" x2="0" y2="1">
						<stop offset="0%" stopColor="#38bdf8" stopOpacity="0.28" />
						<stop offset="100%" stopColor="#38bdf8" stopOpacity="0" />
					</linearGradient>
				</defs>

				<text
					className={styles.playerQuoteChart__axisTitle}
					x={PAD_LEFT}
					y={11}
					textAnchor="start"
				>
					{DEMO_QUOTE_UNIT_LABEL}
				</text>

				{yValues.map((value) => {
					const y =
						PAD_TOP + PLOT_HEIGHT - ((value - minY) / spanY) * PLOT_HEIGHT;

					return (
						<g key={value}>
							<line
								x1={PAD_LEFT}
								y1={y}
								x2={WIDTH - PAD_RIGHT}
								y2={y}
								stroke="#1e293b"
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

				<line
					x1={PAD_LEFT}
					y1={baselineY}
					x2={WIDTH - PAD_RIGHT}
					y2={baselineY}
					stroke="#334155"
					strokeWidth={1}
				/>

				{[...tickIndexes].map((index) => {
					const coordinate = coordinates[index];
					const isAlternate = index % 2 !== 0 && index !== 0;

					return (
						<g
							key={coordinate.point.date}
							className={
								isAlternate
									? styles.playerQuoteChart__tick_alternate
									: undefined
							}
						>
							<text
								className={styles.playerQuoteChart__axisLabel}
								x={coordinate.x}
								y={baselineY + 16}
								textAnchor={index === 0 ? "start" : "middle"}
							>
								{formatDayMonth(coordinate.point.date)}
							</text>
						</g>
					);
				})}

				<path d={areaPath} fill={`url(#${gradientId}-area)`} />
				<path
					d={linePath}
					fill="none"
					stroke={`url(#${gradientId}-line)`}
					strokeWidth={2.5}
					strokeLinecap="round"
					strokeLinejoin="round"
				/>

				{markers.map(
					({
						x,
						y,
						point,
					}: {
						x: number;
						y: number;
						point: TokenQuoteSeries["points"][0];
					}) => (
						<circle
							key={point.date}
							cx={x}
							cy={y}
							r={3}
							fill="#0f172a"
							stroke="#38bdf8"
							strokeWidth={1.5}
						/>
					),
				)}

				{last ? (
					<circle
						cx={last.x}
						cy={last.y}
						r={4.5}
						fill="#38bdf8"
						stroke="#e0f2fe"
						strokeWidth={1.5}
					/>
				) : null}
			</svg>

			<details>
				<summary>Valores de la serie</summary>
				<table>
					<caption className={styles.playerQuoteChart__caption}>
						Cotizaciones diarias de demostración en {DEMO_QUOTE_UNIT_LABEL}
					</caption>
					<thead>
						<tr>
							<th scope="col">Fecha</th>
							<th scope="col">Precio</th>
						</tr>
					</thead>
					<tbody>
						{points.map((point: TokenQuoteSeries["points"][0]) => (
							<tr key={point.date}>
								<td>{point.date}</td>
								<td>{point.price}</td>
							</tr>
						))}
					</tbody>
				</table>
			</details>
			<p className={styles.playerQuoteChart__demo}>{DEMO_DATA_LABEL}</p>
		</section>
	);
}
