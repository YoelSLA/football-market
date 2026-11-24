import { useLocation } from "react-router-dom";
import type { PlayerRankingItem, RankingMode } from "@/features/players/types";
import styles from "./RankingsTable.module.scss";
import { RankingsTableRow } from "./RankingsTableRow";

interface Props {
	items: PlayerRankingItem[];
	mode: RankingMode;
}

export default function RankingsTable({ items, mode }: Props) {
	const location = useLocation();
	const fromLocation = `${location.pathname}${location.search}`;

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
						<RankingsTableRow
							key={`${player.position}-${player.playerId}`}
							player={player}
							mode={mode}
							fromLocation={fromLocation}
						/>
					))}
				</tbody>
			</table>
		</div>
	);
}
