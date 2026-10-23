import { Link } from "react-router-dom";
import {
	DEMO_DATA_LABEL,
	DEMO_MESSAGES,
	getLeagueClassification,
	getPlayerSampleStatistics,
	PLAYER_SAMPLE_STATISTIC_ITEMS,
} from "@/features/players/constants";
import type { Player } from "@/features/players/types";
import NationalityFlag from "./NationalityFlag";
import styles from "./PlayerCard.module.scss";
import PlayerPortrait from "./PlayerPortrait";
import TeamBadge from "./TeamBadge";

const LEAGUE_CLASSES: Record<string, string> = {
	premier: styles.playerCard_premier,
	primera: styles.playerCard_primera,
	"serie-a": styles.playerCard_serieA,
	bundesliga: styles.playerCard_bundesliga,
	"ligue-one": styles.playerCard_ligueOne,
};

interface Props {
	player: Player;
}

export default function PlayerCard({ player }: Props) {
	const league = getLeagueClassification(player.leagueName);
	const sampleStatistics = getPlayerSampleStatistics(player.id);

	return (
		<article
			className={`${styles.playerCard} ${LEAGUE_CLASSES[league.variant]}`}
		>
			<div className={styles.playerCard__visual}>
				<div className={styles.playerCard__identity}>
					{player.position && (
						<span className={styles.playerCard__position}>
							{player.position}
						</span>
					)}
					<TeamBadge key={player.teamName} teamName={player.teamName} />
					<NationalityFlag
						key={player.nationality}
						nationality={player.nationality}
					/>
				</div>
				<span className={styles.playerCard__brand}>FM</span>
				<PlayerPortrait
					key={`${player.id}:${player.imageUrl}:${player.fallbackImageUrl}`}
					player={player}
				/>
			</div>
			<header className={styles.playerCard__header}>
				<h2 className={styles.playerCard__name}>
					<Link
						className={styles.playerCard__link}
						to={`/players/${player.id}`}
						title={DEMO_MESSAGES.playerPortraitAlt.replace(
							"{name}",
							player.name,
						)}
					>
						{player.name}
					</Link>
				</h2>
				<p className={styles.playerCard__team}>{player.teamName}</p>
				<div className={styles.playerCard__league}>
					{league.image && <img src={league.image} alt="" />}
					<span>{player.leagueName}</span>
				</div>
			</header>

			<dl className={styles.playerCard__details}>
				{PLAYER_SAMPLE_STATISTIC_ITEMS.map((statistic) => (
					<div key={statistic.key}>
						<dt>{statistic.label}</dt>
						<dd>{sampleStatistics[statistic.key]}</dd>
					</div>
				))}
			</dl>

			<p className={styles.playerCard__demoLabel}>Activo · {DEMO_DATA_LABEL}</p>
		</article>
	);
}
