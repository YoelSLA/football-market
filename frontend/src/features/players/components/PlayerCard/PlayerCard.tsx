import { useState } from "react";
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

function PlayerPortrait({ player }: PlayerCardProps) {
	const [imageIndex, setImageIndex] = useState(0);
	const images = [player.imageUrl, player.fallbackImageUrl]
		.filter((url): url is string => Boolean(url))
		.filter((url, index, urls) => urls.indexOf(url) === index);
	images.push(genericPlayerImage);

	return (
		<img
			className={styles["player-card__portrait"]}
			src={images[imageIndex]}
			alt=""
			onError={() =>
				setImageIndex((index) => Math.min(index + 1, images.length - 1))
			}
		/>
	);
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
				<PlayerPortrait
					key={`${player.id}:${player.imageUrl}:${player.fallbackImageUrl}`}
					player={player}
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
