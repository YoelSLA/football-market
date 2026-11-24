import type React from "react";
import { Link } from "react-router-dom";
import NationalityFlag from "../NationalityFlag";
import PlayerPortrait from "../PlayerPortrait";
import TeamBadge from "../TeamBadge";
import styles from "./PlayerCardShell.module.scss";

export interface PlayerCardShellProps {
	player: {
		id: number;
		name: string;
		nationality?: string | null;
		imageUrl?: string | null;
		fallbackImageUrl?: string | null;
		teamName?: string | null;
		leagueName?: string | null;
		teamBadgeUrl?: string | null;
		teamId?: number | null;
		leagueId?: number | null;
	};
	positionBadge?: React.ReactNode;
	headerRight?: React.ReactNode;
	headerMeta?: React.ReactNode;
	metricsStrip?: React.ReactNode;
	actions: React.ReactNode;
	frozen?: boolean;
	nameHref?: string;
	nameLinkState?: unknown;
	onNameClick?: () => void;
	className?: string;
}

export default function PlayerCardShell({
	player,
	positionBadge,
	headerRight,
	headerMeta,
	metricsStrip,
	actions,
	frozen = false,
	nameHref,
	nameLinkState,
	onNameClick,
	className,
}: PlayerCardShellProps) {
	const teamName = player.teamName ?? "Sin equipo";
	const leagueName = player.leagueName ?? "Sin liga";

	return (
		<article
			className={`${styles.card} ${frozen ? styles.card_frozen : ""} ${className ?? ""}`.trim()}
		>
			{/* 1. Header con badges flotantes */}
			<div className={styles.card__floatingHeader}>
				<div className={styles.card__badgePos}>
					{positionBadge}
					{player.nationality && (
						<NationalityFlag
							nationality={player.nationality}
							className={styles.card__nationalityFlag}
						/>
					)}
				</div>
				{headerRight}
			</div>

			{/* 2. Escenario Visual Central: Retrato destacado */}
			<div className={styles.card__visual}>
				<PlayerPortrait
					src={player.imageUrl}
					fallbackSrc={player.fallbackImageUrl}
					name={player.name}
					fit="contain"
					className={styles.card__portrait}
				/>
			</div>

			{/* 3. Dock Inferior Traslúcido («Frosted Glass») */}
			<div className={styles.card__glassOverlay}>
				<div className={styles.card__identityAndPrice}>
					<div className={styles.card__identity}>
						<h3 className={styles.card__name} title={player.name}>
							{nameHref ? (
								<Link to={nameHref} state={nameLinkState}>
									{player.name}
								</Link>
							) : onNameClick ? (
								<button
									type="button"
									onClick={onNameClick}
									style={{
										background: "none",
										border: "none",
										padding: 0,
										color: "inherit",
										font: "inherit",
										cursor: "pointer",
										textAlign: "left",
									}}
								>
									{player.name}
								</button>
							) : (
								player.name
							)}
						</h3>
						<div className={styles.card__clubLeague}>
							<TeamBadge
								teamId={player.teamId ?? undefined}
								teamName={teamName}
								leagueId={player.leagueId ?? undefined}
								leagueName={leagueName}
								src={player.teamBadgeUrl}
								className={styles.card__teamBadge}
							/>
							<span className={styles.card__clubText}>{teamName}</span>
						</div>
					</div>

					{headerMeta && (
						<div className={styles.card__metaBlock}>{headerMeta}</div>
					)}
				</div>

				{metricsStrip && (
					<div className={styles.card__metricsStrip}>{metricsStrip}</div>
				)}

				<div className={styles.card__actions}>{actions}</div>
			</div>
		</article>
	);
}
