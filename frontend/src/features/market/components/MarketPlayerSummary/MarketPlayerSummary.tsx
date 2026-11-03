import { useState } from "react";
import type { MarketPlayer } from "@/features/market/types";
import genericPlayerImage from "@/shared/assets/Player Generic-Icon.png";
import { Globe, Shield } from "@/shared/icons";
import { getLeagueShield } from "@/shared/utils/leagueShield";
import { getTeamShield } from "@/shared/utils/teamShield";
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
	const [teamShieldFailed, setTeamShieldFailed] = useState(false);
	const [leagueShieldFailed, setLeagueShieldFailed] = useState(false);

	const images = [player.imageUrl, player.fallbackImageUrl, genericPlayerImage]
		.filter((url): url is string => Boolean(url))
		.filter((url, index, urls) => urls.indexOf(url) === index);
	const position = POSITION_BADGES[player.position] ?? player.position;
	const badgeClass =
		position === "ARQ"
			? styles.marketPlayerSummary__badge_goalkeeper
			: styles.marketPlayerSummary__badge_outfield;

	const teamShield = getTeamShield(player.leagueName, player.teamName);
	const leagueShield = getLeagueShield(player.leagueName);

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
					<span
						className={`${styles.marketPlayerSummary__badge} ${badgeClass}`}
						title={player.position}
					>
						{position}
					</span>
					<h3>{player.name}</h3>
					<p>
						{teamShield && !teamShieldFailed ? (
							<img
								src={teamShield}
								alt=""
								className={styles.marketPlayerSummary__metaIcon}
								aria-hidden="true"
								onError={() => setTeamShieldFailed(true)}
							/>
						) : (
							<Shield aria-hidden="true" />
						)}
						<span>{player.teamName}</span>
					</p>
					<p>
						{leagueShield && !leagueShieldFailed ? (
							<img
								src={leagueShield}
								alt=""
								className={styles.marketPlayerSummary__metaIcon}
								aria-hidden="true"
								onError={() => setLeagueShieldFailed(true)}
							/>
						) : (
							<Globe aria-hidden="true" />
						)}
						<span>{player.leagueName}</span>
					</p>
				</div>
			</div>
		</div>
	);
}
