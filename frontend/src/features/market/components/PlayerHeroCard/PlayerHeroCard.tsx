import type React from "react";
import { PlayerPositionBadge } from "@/features/players";
import {
	CompetitionLogo,
	NationalityFlag,
	PlayerPortrait,
	TeamBadge,
} from "@/shared/components";
import styles from "./PlayerHeroCard.module.scss";

export interface PlayerHeroCardProps {
	player: {
		id: number;
		name: string;
		position: string;
		nationality?: string | null;
		imageUrl?: string | null;
		fallbackImageUrl?: string | null;
		teamName?: string | null;
		leagueName?: string | null;
		teamBadgeUrl?: string | null;
		teamId?: number | null;
		leagueId?: number | null;
	};
	headerRight?: React.ReactNode;
	statBoxLeft: React.ReactNode;
	statBoxRight: React.ReactNode;
	footerContent?: React.ReactNode;
	className?: string;
}

export default function PlayerHeroCard({
	player,
	headerRight,
	statBoxLeft,
	statBoxRight,
	footerContent,
	className,
}: PlayerHeroCardProps) {
	const teamName = player.teamName ?? "Sin equipo";
	const leagueName = player.leagueName ?? "Sin liga";
	const teamId = player.teamId;
	const leagueId = player.leagueId;
	const nationality = player.nationality;

	return (
		<article
			className={`${styles.heroCard} ${className ?? ""}`.trim()}
			aria-label={`Ficha de ${player.name}`}
		>
			{/* 1. Badges flotantes superiores */}
			<div className={styles.heroCard__topBadges}>
				<div className={styles.heroCard__badgePos}>
					<PlayerPositionBadge position={player.position} />
					{nationality && (
						<span className={styles.heroCard__countryPill}>
							<NationalityFlag
								nationality={nationality}
								className={styles.heroCard__countryFlag}
							/>
							<span>{nationality}</span>
						</span>
					)}
				</div>
				{headerRight}
			</div>

			{/* 2. Visual Stage con retrato grande y resplandor ambiental */}
			<div className={styles.heroCard__visual}>
				<div className={styles.heroCard__glow} aria-hidden="true" />
				<PlayerPortrait
					src={player.imageUrl}
					fallbackSrc={player.fallbackImageUrl}
					name={player.name}
					fit="contain"
					className={styles.heroCard__portrait}
				/>
			</div>

			{/* 3. Dock inferior («Frosted Glass») */}
			<div className={styles.heroCard__dock}>
				<div className={styles.heroCard__identity}>
					<h3 className={styles.heroCard__name} title={player.name}>
						{player.name}
					</h3>
					<div className={styles.heroCard__clubLeague}>
						<TeamBadge
							teamId={teamId ?? undefined}
							teamName={teamName}
							leagueId={leagueId ?? undefined}
							leagueName={leagueName}
							src={player.teamBadgeUrl}
							className={styles.heroCard__teamBadge}
						/>
						<span className={styles.heroCard__clubText}>{teamName}</span>
						<span className={styles.heroCard__clubSeparator} aria-hidden="true">
							•
						</span>
						<CompetitionLogo
							competitionId={leagueId ?? undefined}
							competitionName={leagueName}
							className={styles.heroCard__competitionLogo}
							decorative
						/>
						<span className={styles.heroCard__leagueText}>{leagueName}</span>
					</div>
				</div>

				<div className={styles.heroCard__statsGrid}>
					<div className={styles.heroCard__statBox}>{statBoxLeft}</div>
					<div className={styles.heroCard__statBox}>{statBoxRight}</div>
				</div>

				{footerContent}
			</div>
		</article>
	);
}
