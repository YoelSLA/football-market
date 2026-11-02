import type { PlayerRankingItem, RankingMode } from "@/features/players/types";
import styles from "./RankingPlaceValue.module.scss";

interface Props {
	player: PlayerRankingItem;
	mode: RankingMode;
}

function formatValue(player: PlayerRankingItem, mode: RankingMode): string {
	if (mode === "PERFORMANCE") {
		return player.score !== undefined
			? player.score.toFixed(6).replace(".", ",")
			: "0,000000";
	}

	return player.price !== undefined
		? `${player.price.toFixed(2).replace(".", ",")} cr.`
		: "0,00 cr.";
}

export default function RankingPlaceValue({ player, mode }: Props) {
	return (
		<div className={styles.value}>
			<span className={styles.value__label}>
				{mode === "PERFORMANCE" ? "Score" : "Cotización"}
			</span>

			<strong className={styles.value__number}>
				{formatValue(player, mode)}
			</strong>
		</div>
	);
}
