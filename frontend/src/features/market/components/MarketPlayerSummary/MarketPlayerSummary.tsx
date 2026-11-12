import type { MarketPlayer } from "@/features/market/types";
import {
	CompetitionLogo,
	PlayerPortrait,
	TeamBadge,
} from "@/shared/components";
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
	const position = POSITION_BADGES[player.position] ?? player.position;
	const badgeClass =
		position === "ARQ"
			? styles.marketPlayerSummary__badge_goalkeeper
			: styles.marketPlayerSummary__badge_outfield;

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
						<CompetitionLogo
							competitionName={player.leagueName}
							className={styles.marketPlayerSummary__competitionLogo}
							decorative
						/>
						<span>{player.leagueName}</span>
					</p>
				</div>
			</div>
		</div>
	);
}
