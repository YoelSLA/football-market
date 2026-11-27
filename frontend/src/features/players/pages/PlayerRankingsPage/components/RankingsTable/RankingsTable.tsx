import type { ReactNode } from "react";
import { useLocation } from "react-router-dom";
import type { PlayerRankingItem } from "@/features/players/types";
import styles from "./RankingsTable.module.scss";
import { RankingsTableRow } from "./RankingsTableRow";

interface Props {
	items: PlayerRankingItem[];
	toolbar?: ReactNode;
}

export default function RankingsTable({ items, toolbar }: Props) {
	const location = useLocation();
	const fromLocation = `${location.pathname}${location.search}`;

	return (
		<div className={styles.tableWrapper}>
			{toolbar && <div className={styles.tableWrapper__toolbar}>{toolbar}</div>}
			<div className={styles.tableWrapper__container}>
				<table
					className={styles.table}
					aria-label="Tabla de posiciones del ranking"
				>
					<thead>
						<tr>
							<th scope="col" className={styles.th_pos}>
								# Pos
							</th>
							<th scope="col" className={styles.th_player}>
								Jugador
							</th>
							<th scope="col" className={styles.th_score}>
								Score
							</th>
							<th scope="col" className={styles.th_price}>
								Cotización
							</th>
							<th scope="col" className={styles.th_date}>
								Fecha valuación
							</th>
						</tr>
					</thead>
					<tbody>
						{items.map((player) => (
							<RankingsTableRow
								key={`${player.position}-${player.playerId}`}
								player={player}
								fromLocation={fromLocation}
							/>
						))}
					</tbody>
				</table>
			</div>
		</div>
	);
}
