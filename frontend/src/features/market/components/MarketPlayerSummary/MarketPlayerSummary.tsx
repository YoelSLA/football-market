import type {
	MarketPlayer,
	OfficialMarketPlayer,
} from "@/features/market/types";
import {
	CompetitionLogo,
	NationalityFlag,
	PlayerPortrait,
	TeamBadge,
} from "@/shared/components";
import styles from "./MarketPlayerSummary.module.scss";

interface Props {
	player: MarketPlayer | OfficialMarketPlayer;
	variant?: "default" | "officialCard";
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

export default function MarketPlayerSummary({
	player,
	variant = "default",
}: Props) {
	const position = POSITION_BADGES[player.position] ?? player.position;
	const teamName = player.teamName ?? "Sin equipo";
	const leagueName = player.leagueName ?? "Sin liga";
	const isOfficialCard = variant === "officialCard";
	const officialPlayer = "quote" in player ? player : null;
	const badgeClass =
		position === "ARQ"
			? styles.marketPlayerSummary__badge_goalkeeper
			: styles.marketPlayerSummary__badge_outfield;

	return (
		<div
			className={`${styles.marketPlayerSummary} ${isOfficialCard ? styles.marketPlayerSummary_officialCard : ""}`}
		>
			<div className={styles.marketPlayerSummary__identity}>
				<PlayerPortrait
					src={player.imageUrl}
					fallbackSrc={player.fallbackImageUrl}
					name={player.name}
					className={styles.marketPlayerSummary__portrait}
				/>
				<div className={styles.marketPlayerSummary__details}>
					<div className={styles.marketPlayerSummary__heading}>
						<span
							className={`${styles.marketPlayerSummary__badge} ${badgeClass}`}
							title={player.position}
						>
							{position}
						</span>
						<h3>{player.name}</h3>
					</div>
					<p>
						<TeamBadge
							teamId={officialPlayer?.teamId ?? undefined}
							teamName={teamName}
							leagueId={officialPlayer?.leagueId ?? undefined}
							leagueName={leagueName}
							src={officialPlayer?.teamBadgeUrl}
							className={styles.marketPlayerSummary__metaIcon}
						/>
						<span>{teamName}</span>
					</p>
					<p>
						<CompetitionLogo
							competitionId={officialPlayer?.leagueId ?? undefined}
							competitionName={leagueName}
							className={styles.marketPlayerSummary__competitionLogo}
							decorative
						/>
						<span>{leagueName}</span>
					</p>
					{isOfficialCard && (
						<p className={styles.marketPlayerSummary__nationality}>
							<NationalityFlag
								nationality={officialPlayer?.nationality}
								className={styles.marketPlayerSummary__nationalityFlag}
							/>
							<span>{officialPlayer?.nationality ?? "Sin información"}</span>
						</p>
					)}
				</div>
			</div>
		</div>
	);
}
