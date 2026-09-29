import genericPlayerImage from "../../assets/Player Generic-Icon.png";
import {
	getLeagueClassification,
	getPositionClassification,
	PLAYER_SAMPLE_STATISTIC_ITEMS,
	PLAYER_SAMPLE_STATISTICS,
} from "../../constants";
import type { Player } from "../../types";
import styles from "./PlayerCard.module.scss";

interface PlayerCardProps {
	player: Player;
}

export function PlayerCard({ player }: PlayerCardProps) {
	const league = getLeagueClassification(player.league);
	const position = getPositionClassification(player.position);

	return (
		<article
			className={`${styles["player-card"]} ${styles[`player-card--${league.variant}`]}`}
		>
			<span className={styles["player-card__status"]}>Activo</span>
			<header className={styles["player-card__header"]}>
				<img
					className={styles["player-card__portrait"]}
					src={genericPlayerImage}
					alt=""
				/>
				<h2>{player.name}</h2>
			</header>

			<dl className={styles["player-card__details"]}>
				<div className={styles["player-card__team"]}>
					<dt>Equipo</dt>
					<dd title={player.team}>{player.team}</dd>
				</div>
				{PLAYER_SAMPLE_STATISTIC_ITEMS.map((statistic) => (
					<div key={statistic.key}>
						<dt>{statistic.label}</dt>
						<dd>{PLAYER_SAMPLE_STATISTICS[statistic.key]}</dd>
					</div>
				))}
			</dl>

			<footer className={styles["player-card__classifications"]}>
				<div>
					{league.image && <img src={league.image} alt="" />}
					<span>{player.league}</span>
				</div>
				<div className={styles[`player-card__position--${position.variant}`]}>
					{position.image && <img src={position.image} alt="" />}
					<span>{player.position}</span>
				</div>
			</footer>
		</article>
	);
}
