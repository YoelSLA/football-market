import { Link, useLocation } from "react-router-dom";
import type { PlayerRankingItem, RankingMode } from "@/features/players/types";
import {
	CompetitionLogo,
	PlayerPortrait,
	PlayerPositionBadge,
	TeamBadge,
} from "@/shared/components";
import styles from "./RankingPlace.module.scss";

interface Props {
	player?: PlayerRankingItem;
	rank: 1 | 2 | 3;
	mode?: RankingMode;
}

const RANK_LABELS = {
	1: "Oro",
	2: "Plata",
	3: "Bronce",
} as const;

export default function RankingPlace({ player, rank }: Props) {
	const location = useLocation();
	const fromLocation = `${location.pathname}${location.search}`;

	if (!player) {
		return (
			<article
				className={`${styles.card} ${styles[`card_rank${rank}`]} ${styles.card_empty}`}
			>
				<span className={styles.emptyText}>Sin datos de líder #{rank}</span>
			</article>
		);
	}

	const scoreText =
		player.score !== undefined
			? player.score.toFixed(6).replace(".", ",")
			: null;

	const priceText =
		player.price !== undefined
			? `${player.price.toFixed(2).replace(".", ",")} ₵`
			: null;

	return (
		<article
			className={`${styles.card} ${styles[`card_rank${rank}`]}`}
			aria-label={`Puesto #${rank}: ${player.playerName}`}
		>
			<div className={styles.medalCol}>
				<span
					className={`${styles.medalCircle} ${
						styles[`medalCircle_rank${rank}`]
					}`}
				>
					{rank}
				</span>
				<span
					className={`${styles.medalLabel} ${styles[`medalLabel_rank${rank}`]}`}
				>
					{RANK_LABELS[rank]}
				</span>
			</div>

			<div className={styles.avatarWrapper}>
				<PlayerPortrait
					src={player.playerAvatar}
					name={player.playerName}
					size="md"
					className={styles.portrait}
				/>
			</div>

			<div className={styles.body}>
				<div className={styles.nameRow}>
					<Link
						to={`/players/${player.playerId}`}
						state={{ from: fromLocation }}
						className={styles.nameLink}
						title={`Ver detalle de ${player.playerName}`}
					>
						<strong className={styles.name}>{player.playerName}</strong>
					</Link>
					{player.tacticalPosition && (
						<PlayerPositionBadge position={player.tacticalPosition} />
					)}
				</div>

				<div className={styles.affiliations}>
					{player.teamName && (
						<span className={styles.affiliationTeam}>
							<TeamBadge
								teamName={player.teamName}
								src={player.teamBadge ?? undefined}
								className={styles.teamBadge}
							/>
							<span className={styles.teamName}>{player.teamName}</span>
						</span>
					)}
					{player.teamName && player.leagueName && (
						<span className={styles.divider}>·</span>
					)}
					{player.leagueName && (
						<span className={styles.affiliationLeague}>
							<span className={styles.leagueLogoWrapper}>
								<CompetitionLogo
									competitionName={player.leagueName}
									src={player.leagueFlag ?? undefined}
									className={styles.leagueLogo}
									decorative
								/>
							</span>
							<span className={styles.leagueName}>{player.leagueName}</span>
						</span>
					)}
				</div>

				<div className={styles.metricsRow}>
					{scoreText && (
						<span className={styles.scoreVal}>
							Score: <strong>{scoreText}</strong>
						</span>
					)}
					{scoreText && priceText && (
						<span className={styles.metricDivider}>·</span>
					)}
					{priceText && (
						<span className={styles.priceVal}>
							Cotización: <strong>{priceText}</strong>
						</span>
					)}
				</div>
			</div>
		</article>
	);
}
