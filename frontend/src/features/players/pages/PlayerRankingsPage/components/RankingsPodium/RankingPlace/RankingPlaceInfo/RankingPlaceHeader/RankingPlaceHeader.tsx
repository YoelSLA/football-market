import { Link, useLocation } from "react-router-dom";
import type { PlayerRankingItem } from "@/features/players/types";
import { PlayerPositionBadge } from "@/shared/components";
import styles from "./RankingPlaceHeader.module.scss";

interface Props {
	player: PlayerRankingItem;
}

export default function RankingPlaceHeader({ player }: Props) {
	const location = useLocation();
	return (
		<div className={styles.header}>
			<Link
				to={`/players/${player.playerId}`}
				state={{ from: `${location.pathname}${location.search}` }}
				className={styles.header__nameLink}
				title={`Ver ficha detallada de ${player.playerName}`}
			>
				<h2 className={styles.header__name}>{player.playerName}</h2>
			</Link>

			{player.tacticalPosition ? (
				<PlayerPositionBadge position={player.tacticalPosition} />
			) : null}
		</div>
	);
}
