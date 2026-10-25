import type { MarketPlayer } from "@/features/market/types";
import genericPlayerImage from "@/shared/assets/Player Generic-Icon.png";
import { CalendarDays, Footprints, Globe, Goal, Heart, Shield } from "@/shared/icons";
import styles from "./MarketPlayerSummary.module.scss";

interface Props {
	player: MarketPlayer;
}

const POSITION_BADGES: Record<string, string> = {
	Goalkeeper: "ARQ",
	GOALKEEPER: "ARQ",
	Defender: "DEF",
	DEFENCE: "DEF",
	CentreBack: "DEF",
	Midfielder: "MED",
	MIDFIELD: "MED",
	Forward: "DEL",
	OFFENCE: "DEL",
	Attacker: "DEL",
};

export default function MarketPlayerSummary({ player }: Props) {
	const images = [player.imageUrl, player.fallbackImageUrl, genericPlayerImage]
		.filter((url): url is string => Boolean(url))
		.filter((url, index, urls) => urls.indexOf(url) === index);
	const position = POSITION_BADGES[player.position] ?? player.position;
	const badgeClass = position === "ARQ"
		? styles.marketPlayerSummary__badge_goalkeeper
		: styles.marketPlayerSummary__badge_outfield;

	return (
		<div className={styles.marketPlayerSummary}>
			<div className={styles.marketPlayerSummary__identity}>
				<img
					key={`${player.id}:${player.imageUrl}:${player.fallbackImageUrl}`}
					src={images[0]}
					alt={`Retrato de ${player.name}`}
					className={styles.marketPlayerSummary__portrait}
					data-image-index="0"
					loading="lazy"
					decoding="async"
					onError={(event) => {
						const image = event.currentTarget;
						const next = Number(image.dataset.imageIndex ?? 0) + 1;
						if (next < images.length) {
							image.dataset.imageIndex = String(next);
							image.src = images[next];
						}
					}}
				/>
				<div className={styles.marketPlayerSummary__details}>
					<span className={`${styles.marketPlayerSummary__badge} ${badgeClass}`} title={player.position}>{position}</span>
					<h3>{player.name}</h3>
					<p><Shield aria-hidden="true" />{player.teamName}</p>
					<p><Globe aria-hidden="true" />{player.leagueName}</p>
				</div>
				<button type="button" disabled className={styles.marketPlayerSummary__favorite} aria-label="Favoritos: pendiente" title="Favoritos: pendiente">
					<Heart aria-hidden="true" />
				</button>
			</div>
			<div className={styles.marketPlayerSummary__stats} aria-label="Estadísticas deportivas sin datos disponibles">
				<div><CalendarDays aria-hidden="true" /><span>Partidos<strong>Sin datos</strong></span></div>
				<div><Goal aria-hidden="true" /><span>Goles<strong>Sin datos</strong></span></div>
				<div><Footprints aria-hidden="true" /><span>Asistencias<strong>Sin datos</strong></span></div>
			</div>
		</div>
	);
}
