import type { PlayerRankingItem, RankingMode } from "@/features/players/types";
import RankingPlaceHeader from "./RankingPlaceHeader";
import styles from "./RankingPlaceInfo.module.scss";
import RankingPlaceMeta from "./RankingPlaceMeta";
import RankingPlaceValue from "./RankingPlaceValue";

interface Props {
	player: PlayerRankingItem;
	mode: RankingMode;
}

export default function RankingPlaceInfo({ player, mode }: Props) {
	return (
		<div className={styles.info}>
			<RankingPlaceHeader player={player} />

			<RankingPlaceMeta player={player} />

			<RankingPlaceValue player={player} mode={mode} />
		</div>
	);
}
