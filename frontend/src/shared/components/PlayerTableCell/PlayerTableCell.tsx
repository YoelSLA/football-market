import type React from "react";
import { Link } from "react-router-dom";
import CompetitionLogo from "../CompetitionLogo";
import PlayerPortrait from "../PlayerPortrait";
import PlayerPositionBadge from "../PlayerPositionBadge";
import TeamBadge from "../TeamBadge";
import styles from "./PlayerTableCell.module.scss";

export interface PlayerTableCellProps {
	player: {
		id?: number | null;
		name: string;
		imageUrl?: string | null;
		fallbackImageUrl?: string | null;
		teamId?: number | null;
		teamName?: string | null;
		teamBadgeUrl?: string | null;
		leagueId?: number | null;
		leagueName?: string | null;
		leagueLogoUrl?: string | null;
		position?: string | null;
	};
	avatarSize?: "sm" | "md";
	href?: string;
	linkState?: unknown;
	subtext?: React.ReactNode;
	badge?: React.ReactNode;
	className?: string;
}

export default function PlayerTableCell({
	player,
	avatarSize = "md",
	href,
	linkState,
	subtext,
	badge,
	className,
}: PlayerTableCellProps) {
	const portraitClass =
		avatarSize === "sm"
			? `${styles.playerPortrait} ${styles.playerPortrait_sm}`
			: styles.playerPortrait;

	const rootClass = className
		? `${styles.playerCell} ${className}`
		: styles.playerCell;

	const content = (
		<>
			<PlayerPortrait
				src={player.imageUrl}
				fallbackSrc={player.fallbackImageUrl}
				name={player.name}
				size={avatarSize}
				className={portraitClass}
			/>
			<div className={styles.playerInfo}>
				<div className={styles.playerNameRow}>
					{href ? (
						<Link to={href} state={linkState} className={styles.playerLink}>
							<strong className={styles.playerName}>{player.name}</strong>
						</Link>
					) : (
						<strong className={styles.playerName}>{player.name}</strong>
					)}
					{player.position && (
						<PlayerPositionBadge position={player.position} />
					)}
					{badge}
				</div>
				<div className={styles.affiliations}>
					{player.teamName && (
						<span className={styles.affiliationTeam}>
							<TeamBadge
								teamId={player.teamId ?? undefined}
								teamName={player.teamName}
								leagueId={player.leagueId ?? undefined}
								leagueName={player.leagueName ?? undefined}
								src={player.teamBadgeUrl ?? undefined}
								className={styles.teamBadge}
							/>
							<span>{player.teamName}</span>
						</span>
					)}
					{player.teamName && player.leagueName && (
						<span className={styles.metaDivider}>·</span>
					)}
					{player.leagueName && (
						<span className={styles.affiliationLeague}>
							<CompetitionLogo
								competitionId={player.leagueId ?? undefined}
								competitionName={player.leagueName}
								src={player.leagueLogoUrl ?? undefined}
								className={styles.leagueLogo}
								decorative
							/>
							<span>{player.leagueName}</span>
						</span>
					)}
					{subtext}
				</div>
			</div>
		</>
	);

	return <div className={rootClass}>{content}</div>;
}
