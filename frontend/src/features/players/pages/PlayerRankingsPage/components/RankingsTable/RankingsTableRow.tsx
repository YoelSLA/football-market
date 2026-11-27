import type { PlayerRankingItem } from "@/features/players/types";
import { PlayerTableCell } from "@/shared/components";
import styles from "./RankingsTable.module.scss";
import TableMedal from "./TableMedal";

interface Props {
	player: PlayerRankingItem;
	fromLocation: string;
}

export function RankingsTableRow({ player, fromLocation }: Props) {
	const scoreText =
		player.score !== undefined
			? player.score.toFixed(6).replace(".", ",")
			: "—";

	const scorePercent =
		player.score !== undefined
			? Math.min(100, Math.max(0, player.score * 100))
			: 0;

	const priceText =
		player.price !== undefined
			? `${player.price.toFixed(2).replace(".", ",")} ₵`
			: "—";

	return (
		<tr className={styles.row}>
			<td className={styles.td_pos}>
				{player.position <= 3 ? (
					<div className={styles.medalWrapper}>
						<TableMedal rank={player.position as 1 | 2 | 3} />
					</div>
				) : (
					<span className={styles.rankNumber}>{player.position}</span>
				)}
			</td>

			<td className={styles.td_player}>
				<PlayerTableCell
					player={{
						id: player.playerId,
						name: player.playerName,
						imageUrl: player.playerAvatar,
						position: player.tacticalPosition ?? undefined,
						teamName: player.teamName,
						teamBadgeUrl: player.teamBadge,
						leagueName: player.leagueName,
						leagueLogoUrl: player.leagueFlag,
					}}
					href={`/players/${player.playerId}`}
					linkState={{ from: fromLocation }}
				/>
			</td>

			<td className={styles.td_score}>
				<div className={styles.scoreContainer}>
					<strong className={styles.scoreValue}>{scoreText}</strong>
					{player.score !== undefined && (
						<div
							className={styles.scoreBar}
							title={`Score: ${scorePercent.toFixed(1)}%`}
						>
							<div
								className={styles.scoreBarFill}
								style={{ width: `${scorePercent}%` }}
							/>
						</div>
					)}
				</div>
			</td>

			<td className={styles.td_price}>
				<div className={styles.priceContainer}>
					<span className={styles.priceValue}>{priceText}</span>
					<span className={styles.priceUnit}>/ token</span>
				</div>
			</td>

			<td className={styles.td_date}>
				<span className={styles.dateValue}>{player.formattedDate}</span>
			</td>
		</tr>
	);
}
