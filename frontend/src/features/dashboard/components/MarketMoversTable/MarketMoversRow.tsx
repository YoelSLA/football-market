import { Link } from "react-router-dom";
import { PlayerPortrait, TeamBadge } from "@/shared/components";
import type { PlayerMover } from "../../types/models";
import styles from "./MarketMoversTable.module.scss";

interface Props {
	mover: PlayerMover;
	rank: number;
}

export function MarketMoversRow({ mover, rank }: Props) {
	const isGainer = (mover.deltaPercentage ?? 0) >= 0;

	const formatPrice = (price: number) => {
		return `${price.toLocaleString("es-ES", {
			minimumFractionDigits: 2,
			maximumFractionDigits: 2,
		})} ₵`;
	};

	const formatDeltaPct = (pct: number | null) => {
		if (pct === null) return "—";
		const sign = pct > 0 ? "+" : "";
		return `${sign}${pct.toFixed(2)}%`;
	};

	const formatDeltaCredits = (delta: number | null) => {
		if (delta === null) return "—";
		const sign = delta > 0 ? "+" : "";
		return `${sign}${delta.toFixed(2)} ₵`;
	};

	return (
		<tr className={styles.row}>
			<td className={styles.tdRank}>{rank}</td>
			<td className={styles.tdPlayer}>
				<Link to={`/players/${mover.playerId}`} className={styles.playerLink}>
					<PlayerPortrait
						src={mover.imageUrl}
						name={mover.name}
						size="sm"
						className={styles.portrait}
					/>
					<div className={styles.playerInfo}>
						<span className={styles.playerName}>{mover.name}</span>
						<span className={styles.playerMeta}>
							<TeamBadge
								teamName={mover.team}
								leagueName={mover.league}
								className={styles.teamBadge}
							/>
							{mover.team} · {mover.position}
						</span>
					</div>
				</Link>
			</td>
			<td className={styles.tdPrice}>{formatPrice(mover.currentPrice)}</td>
			<td className={styles.tdDeltaPct}>
				<span
					className={`${styles.deltaBadge} ${
						isGainer ? styles.gainBadge : styles.lossBadge
					}`}
				>
					{formatDeltaPct(mover.deltaPercentage)}
				</span>
			</td>
			<td
				className={`${styles.tdDeltaCredits} ${
					isGainer ? styles.gainText : styles.lossText
				}`}
			>
				{formatDeltaCredits(mover.deltaCredits)}
			</td>
		</tr>
	);
}
