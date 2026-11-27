import type { MarketPlayer } from "@/features/market/types";
import { PlayerPositionBadge } from "@/features/players";
import {
	CompetitionLogo,
	NationalityFlag,
	PlayerPortrait,
	TeamBadge,
} from "@/shared/components";
import styles from "./MarketPlayerSummary.module.scss";

interface Props {
	player: MarketPlayer;
	variant?: "default" | "officialCard" | "communityCard";
}

export default function MarketPlayerSummary({
	player,
	variant = "default",
}: Props) {
	const teamName = player.teamName ?? "Sin equipo";
	const leagueName = player.leagueName ?? "Sin liga";
	const isCatalogCard =
		variant === "officialCard" || variant === "communityCard";
	const teamId = player.teamId;
	const leagueId = player.leagueId;
	const nationality = player.nationality;

	const team = (
		<span className={styles.marketPlayerSummary__metaItem}>
			<TeamBadge
				teamId={teamId ?? undefined}
				teamName={teamName}
				leagueId={leagueId ?? undefined}
				leagueName={leagueName}
				src={player.teamBadgeUrl}
				className={styles.marketPlayerSummary__metaIcon}
			/>
			<span className={styles.marketPlayerSummary__metaText}>{teamName}</span>
		</span>
	);

	const league = (
		<span className={styles.marketPlayerSummary__metaItem}>
			<CompetitionLogo
				competitionId={leagueId ?? undefined}
				competitionName={leagueName}
				className={styles.marketPlayerSummary__competitionLogo}
				decorative
			/>
			<span className={styles.marketPlayerSummary__metaText}>{leagueName}</span>
		</span>
	);

	return (
		<div
			className={`${styles.marketPlayerSummary} ${
				isCatalogCard ? styles.marketPlayerSummary_officialCard : ""
			}`}
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
						<PlayerPositionBadge
							position={player.position}
							className={styles.marketPlayerSummary__badge}
						/>

						<h3>{player.name}</h3>
					</div>

					{isCatalogCard ? (
						<div className={styles.marketPlayerSummary__clubLeague}>
							{team}

							<span
								className={styles.marketPlayerSummary__separator}
								aria-hidden="true"
							>
								·
							</span>

							{league}
						</div>
					) : (
						<>
							<p>{team}</p>
							<p>{league}</p>
						</>
					)}

					{isCatalogCard && (
						<p className={styles.marketPlayerSummary__nationality}>
							<NationalityFlag
								nationality={nationality}
								className={styles.marketPlayerSummary__nationalityFlag}
							/>
							<span>{nationality ?? "Sin información"}</span>
						</p>
					)}
				</div>
			</div>
		</div>
	);
}
