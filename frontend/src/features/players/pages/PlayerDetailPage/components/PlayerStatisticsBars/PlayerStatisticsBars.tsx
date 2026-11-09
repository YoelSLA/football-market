import { formatStatisticsValue } from "@/features/players/utils";
import styles from "./PlayerStatisticsBars.module.scss";

export interface BarItem {
	key: string;
	label: string;
	value: number | null;
	unit?: string;
}

interface Props {
	bars: BarItem[];
	maxValue?: number;
	ariaLabel?: string;
}

export default function PlayerStatisticsBars({
	bars,
	maxValue: propMaxValue,
	ariaLabel = "Gráficos de barras estadísticas",
}: Props) {
	const computedMax = Math.max(
		0,
		...bars.filter((b) => b.value !== null).map((b) => b.value as number),
	);
	const maxValue = propMaxValue ?? computedMax;

	return (
		<section className={styles.bars} aria-label={ariaLabel}>
			{bars.map((bar) => {
				const isNull = bar.value === null;
				const displayValue = formatStatisticsValue(bar.value);
				const percentage =
					!isNull && maxValue > 0
						? Math.min(
								100,
								Math.max(0, ((bar.value as number) / maxValue) * 100),
							)
						: 0;

				return (
					<div className={styles.barRow} key={bar.key}>
						<span className={styles.barLabel} title={bar.label}>
							{bar.label}
						</span>
						<div
							className={styles.barTrack}
							role="progressbar"
							aria-valuenow={isNull ? undefined : (bar.value ?? 0)}
							aria-valuemin={0}
							aria-valuemax={maxValue > 0 ? maxValue : 100}
							aria-label={`${bar.label}: ${displayValue}`}
						>
							<div
								className={styles.barFill}
								style={{ width: `${percentage}%` }}
							/>
						</div>
						<span
							className={`${styles.barValue} ${isNull ? styles.barValueEmpty : ""}`}
						>
							{displayValue}
							{bar.unit === "%" && !isNull ? " %" : ""}
						</span>
					</div>
				);
			})}
		</section>
	);
}
