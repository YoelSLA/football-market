import { Link } from "react-router-dom";
import type { PlayerRankingItem, RankingMode } from "@/features/players/types";
import {
	NationalityFlag,
	PlayerPortrait,
	TeamBadge,
} from "@/shared/components";
import styles from "./RankingsTable.module.scss";
import TableMedal from "./TableMedal";

interface Props {
	items: PlayerRankingItem[];
	mode: RankingMode;
}

export default function RankingsTable({ items, mode }: Props) {
	const formatValue = (player: PlayerRankingItem) => {
		if (mode === "PERFORMANCE") {
			return player.score !== undefined
				? player.score.toFixed(6).replace(".", ",")
				: "0,000000";
		}
		return player.price !== undefined
			? `${player.price.toFixed(2).replace(".", ",")} cr.`
			: "0,00 cr.";
	};

	return (
		<div className={styles.tableWrapper}>
			<table
				className={`${styles.table} ${
					mode === "VALUATION"
						? styles.table_valuation
						: styles.table_performance
				}`}
				aria-label="Tabla de posiciones del ranking"
			>
				<thead>
					<tr>
						<th scope="col" className={styles.th_pos}>
							#
						</th>
						<th scope="col" className={styles.th_player}>
							Jugador
						</th>
						<th scope="col" className={styles.th_team}>
							Equipo
						</th>
						<th scope="col" className={styles.th_league}>
							Liga
						</th>
						<th scope="col" className={styles.th_role}>
							Posición
						</th>
						<th
							scope="col"
							className={`${styles.th_value} ${styles.th_activeSort}`}
						>
							<span>{mode === "PERFORMANCE" ? "Score" : "Cotización"}</span>
							<span className={styles.sortArrow} aria-hidden="true">
								▾
							</span>
						</th>
						<th scope="col" className={styles.th_date}>
							{mode === "PERFORMANCE" ? "Fecha de valuación" : "Vigencia"}
						</th>
						{mode === "VALUATION" && (
							<th scope="col" className={styles.th_origin}>
								Origen
							</th>
						)}
					</tr>
				</thead>
				<tbody>
					{items.map((player) => (
						<tr
							key={`${player.position}-${player.playerId}`}
							className={styles.row}
						>
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
										teamName={player.teamName}
										leagueName={player.leagueName}
										className={styles.teamBadge}
									/>
									<span className={styles.teamName}>{player.teamName}</span>
								</div>
							</td>

							<td className={styles.td_league}>
								<div className={styles.leagueContainer}>
									{player.leagueFlag && (
										<NationalityFlag
											src={player.leagueFlag}
											nationality={player.leagueName}
											className={styles.leagueFlag}
										/>
									)}
									<span className={styles.leagueName}>{player.leagueName}</span>
								</div>
							</td>

							<td className={styles.td_role}>
								<span
									className={`${styles.positionBadge} ${
										styles[`positionBadge_${player.tacticalPosition}`]
									}`}
								>
									{player.tacticalPosition}
								</span>
							</td>

							<td className={styles.td_value}>
								<strong className={styles.valueStrong}>
									{formatValue(player)}
								</strong>
							</td>

							<td className={styles.td_date}>
								<span className={styles.dateText}>{player.formattedDate}</span>
							</td>

							{mode === "VALUATION" && (
								<td className={styles.td_origin}>
									{player.origin && (
										<span
											className={`${styles.originChip} ${
												player.origin === "RECOMPUTED"
													? styles.originChip_recomputed
													: styles.originChip_carried
											}`}
										>
											{player.origin}
										</span>
									)}
								</td>
							)}
						</tr>
					))}
				</tbody>
			</table>
		</div>
	);
}
