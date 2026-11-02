import { Link } from "react-router-dom";
import type { PlayerRankingItem } from "@/features/players/types";
import styles from "./RankingPlaceHeader.module.scss";

interface Props {
	player: PlayerRankingItem;
}

export default function RankingPlaceHeader({ player }: Props) {
	return (
		<div className={styles.header}>
			<Link
				to={`/players/${player.playerId}`}
				className={styles.header__nameLink}
				title={`Ver ficha detallada de ${player.playerName}`}
			>
				<h2 className={styles.header__name}>{player.playerName}</h2>
			</Link>

			<span
				className={`${styles.positionBadge} ${
					styles[`positionBadge_${player.tacticalPosition}`] ?? ""
				}`}
				title={`Posición: ${player.tacticalPosition}`}
			>
				{player.tacticalPosition}
			</span>
		</div>
	);
}
