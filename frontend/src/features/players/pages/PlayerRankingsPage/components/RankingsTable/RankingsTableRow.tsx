import { Link } from "react-router-dom";
import type {
	PlayerRankingItem,
	RankingMode,
	RankingOrigin,
} from "@/features/players/types";
import {
	CompetitionLogo,
	PlayerPortrait,
	TeamBadge,
} from "@/shared/components";
import styles from "./RankingsTable.module.scss";
import TableMedal from "./TableMedal";

const ORIGIN_CLASSES: Record<RankingOrigin, string> = {
	INITIAL: styles.originChip_carried,
	RECOMPUTED: styles.originChip_recomputed,
	CARRIED_FORWARD: styles.originChip_carried,
};

interface Props {
	player: PlayerRankingItem;
	mode: RankingMode;
	fromLocation: string;
}

export function RankingsTableRow({ player, mode, fromLocation }: Props) {
	const formatValue = (p: PlayerRankingItem) => {
		if (mode === "PERFORMANCE") {
			return p.score !== undefined
				? p.score.toFixed(6).replace(".", ",")
				: "0,000000";
		}
		return p.price !== undefined
			? `${p.price.toFixed(2).replace(".", ",")} ₵`
			: "0,00 ₵";
	};

	return (
		<tr className={styles.row}>
			<td className={styles.td_pos}>
				{player.position <= 3 ? (
					<div className={styles.medalWrapper}>
						<TableMedal rank={player.position as 1 | 2 | 3} />
					</div>
				) : (
					<span className={styles.posNumber}>{player.position}</span>
				)}
			</td>

			<td className={styles.td_player}>
				<Link
					to={`/players/${player.playerId}`}
					state={{ from: fromLocation }}
					className={styles.playerLink}
					title={`Ver detalle de ${player.playerName}`}
				>
					<PlayerPortrait
						src={player.playerAvatar}
						name={player.playerName}
						alt=""
						className={styles.playerAvatar}
					/>
					<span className={styles.playerName}>{player.playerName}</span>
				</Link>
			</td>

			<td className={styles.td_team}>
				<div className={styles.teamContainer}>
					<TeamBadge
						src={player.teamBadge}
						teamName={player.teamName ?? "Equipo no disponible"}
						leagueName={player.leagueName ?? undefined}
						className={styles.teamBadge}
					/>
					<span className={styles.teamName}>{player.teamName ?? "—"}</span>
				</div>
			</td>

			<td className={styles.td_league}>
				<div className={styles.leagueContainer}>
					<CompetitionLogo
						src={player.leagueFlag}
						competitionName={player.leagueName ?? "Liga"}
						className={styles.leagueFlag}
						decorative
					/>
					<span className={styles.leagueName}>{player.leagueName ?? "—"}</span>
				</div>
			</td>

			<td className={styles.td_role}>
				{player.tacticalPosition ? (
					<span
						className={`${styles.positionBadge} ${
							styles[`positionBadge_${player.tacticalPosition}`]
						}`}
					>
						{player.tacticalPosition}
					</span>
				) : (
					<span className={styles.positionBadge}>—</span>
				)}
			</td>

			<td className={styles.td_value}>
				<strong className={styles.valueStrong}>{formatValue(player)}</strong>
			</td>

			<td className={styles.td_date}>
				<span className={styles.dateText}>{player.formattedDate}</span>
			</td>

			{mode === "VALUATION" && (
				<td className={styles.td_origin}>
					{player.origin && (
						<span
							className={`${styles.originChip} ${
								ORIGIN_CLASSES[player.origin]
							}`}
						>
							{player.origin}
						</span>
					)}
				</td>
			)}
		</tr>
	);
}
