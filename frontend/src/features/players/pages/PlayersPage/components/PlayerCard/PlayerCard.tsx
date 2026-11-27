import { Link, useLocation } from "react-router-dom";
import PlayerPositionBadge from "@/features/players/components/PlayerPositionBadge";
import { getLeagueClassification } from "@/features/players/constants";
import type { Player } from "@/features/players/types";
import { CompetitionLogo, PlayerCardShell } from "@/shared/components";
import styles from "./PlayerCard.module.scss";

interface Props {
	player: Player;
}

const number = new Intl.NumberFormat("es-AR", { maximumFractionDigits: 2 });

export default function PlayerCard({ player }: Props) {
	const location = useLocation();
	const league = getLeagueClassification(player.leagueName);
	const stats = player.statistics;
	const hasGoals = stats?.goalsTotal != null && stats.goalsTotal > 0;
	const hasAssists = stats?.assistsTotal != null && stats.assistsTotal > 0;

	const headerRight = (
		<div className={styles.leagueBadge} title={player.leagueName}>
			<CompetitionLogo
				competitionName={player.leagueName}
				src={league.image}
				className={styles.leagueIcon}
				decorative
			/>
			<span className={styles.leagueText}>{player.leagueName}</span>
		</div>
	);

	const headerMeta = (
		<div className={styles.statusBadge}>
			<span className={styles.statusDot} aria-hidden="true" />
			<span>Activo</span>
		</div>
	);

	const metricsStrip = (
		<div className={styles.statsStrip}>
			<div className={`${styles.statItem} ${styles.statDivider}`}>
				<span className={styles.statLabel}>Partidos</span>
				<strong className={styles.statValue}>
					{stats?.appearancesTotal != null
						? number.format(stats.appearancesTotal)
						: "—"}
				</strong>
			</div>
			<div className={`${styles.statItem} ${styles.statDivider}`}>
				<span className={styles.statLabel}>Goles</span>
				<strong
					className={`${styles.statValue} ${
						hasGoals ? styles.statValue_gain : ""
					}`}
				>
					{stats?.goalsTotal != null ? number.format(stats.goalsTotal) : "—"}
				</strong>
			</div>
			<div className={styles.statItem}>
				<span className={styles.statLabel}>Asistencias</span>
				<strong
					className={`${styles.statValue} ${
						hasAssists ? styles.statValue_highlight : ""
					}`}
				>
					{stats?.assistsTotal != null
						? number.format(stats.assistsTotal)
						: "—"}
				</strong>
			</div>
		</div>
	);

	const actions = (
		<Link
			to={`/players/${player.id}`}
			state={{ from: `${location.pathname}${location.search}` }}
			className={styles.detailButton}
			aria-label={`Ver ficha de ${player.name}`}
		>
			<span>Ver ficha de jugador</span>
			<span className={styles.actionArrow} aria-hidden="true">
				→
			</span>
		</Link>
	);

	return (
		<PlayerCardShell
			player={player}
			positionBadge={<PlayerPositionBadge position={player.position} />}
			headerRight={headerRight}
			headerMeta={headerMeta}
			metricsStrip={metricsStrip}
			actions={actions}
			nameHref={`/players/${player.id}`}
			nameLinkState={{ from: `${location.pathname}${location.search}` }}
		/>
	);
}
