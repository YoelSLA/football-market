import type { PlayerRankingItem, RankingMode } from "@/features/players/types";
import styles from "./RankingPlace.module.scss";
import RankingPlaceEmpty from "./RankingPlaceEmpty";
import RankingPlaceInfo from "./RankingPlaceInfo";
import RankingPlacePortrait from "./RankingPlacePortrait";
import RankingPlaceRank from "./RankingPlaceRank";

interface Props {
	player?: PlayerRankingItem;
	rank: 1 | 2 | 3;
	mode: RankingMode;
}

export default function RankingPlace({ player, rank, mode }: Props) {
	if (!player) {
		return <RankingPlaceEmpty rank={rank} />;
	}

	return (
		<article
			className={`${styles.place} ${styles[`place_rank${rank}`]}`}
			aria-label={`Puesto #${rank}: ${player.playerName}`}
		>
			{player.teamBadge && (
				<img
					src={player.teamBadge}
					alt=""
					aria-hidden="true"
					className={styles.place__watermark}
				/>
			)}

			<RankingPlaceRank rank={rank} />

			<RankingPlacePortrait
				src={player.playerAvatar}
				name={player.playerName}
			/>

			<RankingPlaceInfo player={player} mode={mode} />
		</article>
	);
}
