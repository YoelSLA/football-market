import styles from "./PlayerStatisticsBars.module.scss";

interface BarProps {
	label: string;
	value: number | null;
	unit: string;
	percentage: number;
	source: "JSON" | "SCRAPING" | null;
}

function Bar({ label, value, unit, percentage, source }: BarProps) {
	const displayValue = value === null ? "No disponible" : `${value} ${unit}`;
	const sourceLabel = source ? ` (${source})` : "";

	return (
		<div className={styles.barContainer}>
			<div className={styles.barHeader}>
				<span className={styles.barLabel}>{label}</span>
				<span className={styles.barValue}>
					{displayValue}
					{sourceLabel}
				</span>
			</div>
			<div
				className={styles.barTrack}
				role="img"
				aria-label={`${label}: ${displayValue}${sourceLabel}`}
			>
				<div
					className={styles.barFill}
					style={{ width: `${Math.min(percentage, 100)}%` }}
				/>
			</div>
		</div>
	);
}

interface Props {
	bars: Array<{
		key: string;
		label: string;
		value: number | null;
		unit: string;
		source: "JSON" | "SCRAPING" | null;
	}>;
	maxValue: number;
}

export default function PlayerStatisticsBars({ bars, maxValue }: Props) {
	return (
		<section className={styles.bars} aria-label="Gráficos de barras">
			{bars.map((bar) => (
				<Bar
					key={bar.key}
					label={bar.label}
					value={bar.value}
					unit={bar.unit}
					percentage={
						maxValue > 0 && bar.value !== null
							? (bar.value / maxValue) * 100
							: 0
					}
					source={bar.source}
				/>
			))}
		</section>
	);
}
