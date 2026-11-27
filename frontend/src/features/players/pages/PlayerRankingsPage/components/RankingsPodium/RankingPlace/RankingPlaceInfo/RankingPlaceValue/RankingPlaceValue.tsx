import type { PlayerRankingItem, RankingMode } from "@/features/players/types";
import styles from "./RankingPlaceValue.module.scss";

interface Props {
	player: PlayerRankingItem;
	mode?: RankingMode;
}

export default function RankingPlaceValue({ player }: Props) {
	const scoreText =
		player.score !== undefined
			? player.score.toFixed(6).replace(".", ",")
			: null;

	const priceText =
		player.price !== undefined
			? `${player.price.toFixed(2).replace(".", ",")} ₵`
			: null;

	return (
		<div className={styles.value}>
			<div className={styles.value__row}>
				{scoreText && (
					<div className={styles.metric}>
						<span className={styles.metricLabel}>Score</span>
						<strong className={styles.metricScore}>{scoreText}</strong>
					</div>
				)}
				{scoreText && priceText && <span className={styles.divider}>·</span>}
				{priceText && (
					<div className={styles.metric}>
						<span className={styles.metricLabel}>Cotización</span>
						<strong className={styles.metricPrice}>{priceText}</strong>
					</div>
				)}
			</div>
		</div>
	);
}
