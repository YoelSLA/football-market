import { formatCredits } from "@/features/portfolio/utils";
import { Coins, TrendingUp } from "@/shared/icons";
import styles from "./PositionDetailMetrics.module.scss";

interface Props {
	acquisitionCost: number | null;
	currentValue: number | null;
	totalProfitLoss: number | null;
}

export default function PositionDetailMetrics({
	acquisitionCost,
	currentValue,
	totalProfitLoss,
}: Props) {
	const resultToneClass =
		totalProfitLoss == null || totalProfitLoss === 0
			? styles.metric_neutral
			: totalProfitLoss > 0
				? styles.metric_positive
				: styles.metric_negative;

	return (
		<div className={styles.metrics}>
			<section className={`${styles.metric} ${styles.metric_cost}`}>
				<div>
					<Coins aria-hidden="true" />
					<h3>Costo de adquisición</h3>
				</div>
				<strong>{formatCredits(acquisitionCost, true)}</strong>
			</section>

			<section className={`${styles.metric} ${styles.metric_value}`}>
				<div>
					<TrendingUp aria-hidden="true" />
					<h3>Valor actual</h3>
				</div>
				<strong>{formatCredits(currentValue, true)}</strong>
			</section>

			<section
				className={`${styles.metric} ${styles.metric_result} ${resultToneClass}`}
			>
				<div>
					<TrendingUp aria-hidden="true" />
					<h3>Resultado total</h3>
				</div>
				<strong>{formatCredits(totalProfitLoss, true)}</strong>
			</section>
		</div>
	);
}
