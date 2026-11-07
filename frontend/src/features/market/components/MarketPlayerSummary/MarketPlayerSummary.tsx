import { useState } from "react";
import type { MarketPlayer } from "@/features/market/types";
import { PlayerPortrait, TeamBadge } from "@/shared/components";
import { Globe } from "@/shared/icons";
import { getLeagueShield } from "@/shared/utils/leagueShield";
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
	const [leagueShieldFailed, setLeagueShieldFailed] = useState(false);

	const position = POSITION_BADGES[player.position] ?? player.position;
	const badgeClass =
		position === "ARQ"
			? styles.marketPlayerSummary__badge_goalkeeper
			: styles.marketPlayerSummary__badge_outfield;

	const leagueShield = getLeagueShield(player.leagueName);

	return (
		<div className={styles.marketPlayerSummary}>
			<div className={styles.marketPlayerSummary__identity}>
				<PlayerPortrait
					src={player.imageUrl}
					fallbackSrc={player.fallbackImageUrl}
					name={player.name}
					className={styles.marketPlayerSummary__portrait}
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
						<TeamBadge
							teamName={player.teamName}
							leagueName={player.leagueName}
							className={styles.marketPlayerSummary__metaIcon}
						/>
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
