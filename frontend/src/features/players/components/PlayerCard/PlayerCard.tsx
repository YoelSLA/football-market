import { useState } from "react";
import { Link } from "react-router-dom";
import genericPlayerImage from "../../assets/Player Generic-Icon.png";
import {
	DEMO_DATA_LABEL,
	DEMO_MESSAGES,
	getLeagueClassification,
	getPlayerSampleStatistics,
	PLAYER_SAMPLE_STATISTIC_ITEMS,
} from "../../constants";
import type { Player } from "../../types";
import { getNationalityFlag, getTeamLogo } from "../../utils";
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
			loading="lazy"
			decoding="async"
			onError={() =>
				setImageIndex((index) => Math.min(index + 1, images.length - 1))
			}
		/>
	);
}

function NationalityFlag({ nationality }: { nationality: string | null }) {
	const url = getNationalityFlag(nationality);
	const [failed, setFailed] = useState(false);
	return url && !failed ? (
		<img
			className={styles["player-card__flag"]}
			src={url}
			alt={nationality ?? ""}
			loading="lazy"
			onError={() => setFailed(true)}
		/>
	) : (
		<span
			className={styles["player-card__flag-placeholder"]}
			role="img"
			aria-label={
				nationality
					? `Bandera no disponible: ${nationality}`
					: "Nacionalidad no disponible"
			}
			title={nationality ?? "Nacionalidad no disponible"}
		>
			—
		</span>
	);
}

function TeamBadge({ teamName }: { teamName: string }) {
	const url = getTeamLogo(teamName);
	const [failed, setFailed] = useState(false);
	return url && !failed ? (
		<img
			className={styles["player-card__badge"]}
			src={url}
			alt={`Escudo de ${teamName}`}
			loading="lazy"
			decoding="async"
			onError={() => setFailed(true)}
		/>
	) : (
		<span
			className={styles["player-card__badge-placeholder"]}
			role="img"
			aria-label={`Escudo no disponible: ${teamName}`}
			title="Escudo no disponible"
		>
			◇
		</span>
	);
}

export function PlayerCard({ player }: PlayerCardProps) {
	const league = getLeagueClassification(player.leagueName);
	const sampleStatistics = getPlayerSampleStatistics(player.id);

	return (
		<article
			className={`${styles["player-card"]} ${styles[`player-card--${league.variant}`]}`}
		>
			<div className={styles["player-card__visual"]}>
				<div className={styles["player-card__identity"]}>
					{player.position && (
						<span className={styles["player-card__position"]}>
							{player.position}
						</span>
					)}
					<TeamBadge key={player.teamName} teamName={player.teamName} />
					<NationalityFlag
						key={player.nationality}
						nationality={player.nationality}
					/>
				</div>
				<span className={styles["player-card__brand"]}>FM</span>
				<PlayerPortrait
					key={`${player.id}:${player.imageUrl}:${player.fallbackImageUrl}`}
					player={player}
				/>
			</div>
			<header className={styles["player-card__header"]}>
				<h2 className={styles["player-card__name"]}>
					<Link
						className={styles["player-card__link"]}
						to={`/players/${player.id}`}
						title={DEMO_MESSAGES.playerPortraitAlt.replace(
							"{name}",
							player.name,
						)}
					>
						{player.name}
					</Link>
				</h2>
				<p className={styles["player-card__team"]}>{player.teamName}</p>
				<div className={styles["player-card__league"]}>
					{league.image && <img src={league.image} alt="" />}
					<span>{player.leagueName}</span>
				</div>
			</header>

			<dl className={styles["player-card__details"]}>
				{PLAYER_SAMPLE_STATISTIC_ITEMS.map((statistic) => (
					<div key={statistic.key}>
						<dt>{statistic.label}</dt>
						<dd>{sampleStatistics[statistic.key]}</dd>
					</div>
				))}
			</dl>

			<p className={styles["player-card__demo-label"]}>
				Activo · {DEMO_DATA_LABEL}
			</p>
		</article>
	);
}
