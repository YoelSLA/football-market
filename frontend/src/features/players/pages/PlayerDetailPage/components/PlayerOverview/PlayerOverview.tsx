import { getLeagueClassification } from "@/features/players/constants";
import type { PlayerDetail, PlayerStatistics } from "@/features/players/types";
import {
	formatStatisticsValue,
	getStatisticsMetric,
	getStatisticsPosition,
	getStatisticsPositionLabel,
} from "@/features/players/utils";
import {
	NationalityFlag,
	PlayerPortrait,
	TeamBadge,
} from "@/shared/components";
import { Shield } from "@/shared/icons";
import styles from "./PlayerOverview.module.scss";

interface Props {
	player: PlayerDetail;
	seasonLabel: string | null;
	extractionDate: string | null;
	statistics: PlayerStatistics | undefined;
}

const HEADLINE_METRIC_KEYS = [
	[
		"saves_per_match",
		"pass_completion_percentage",
		"successful_long_balls_per_match",
	],
	["tackles_per_match", "interceptions_per_match", "clearances_per_match"],
	["passes_per_match", "pass_completion_percentage", "key_passes_per_match"],
	["goals_total", "shots_per_match", "assists_total"],
] as const;

function formatDate(dateStr: string | null): string {
	if (!dateStr) return "No disponible";
	const parts = dateStr.split("-");
	if (parts.length === 3) {
		const [year, month, day] = parts;
		return `${day}/${month}/${year}`;
	}
	return dateStr;
}

export default function PlayerOverview({
	player,
	seasonLabel,
	extractionDate,
	statistics,
}: Props) {
	const league = getLeagueClassification(player.leagueName);
	const positionLabel = getStatisticsPositionLabel(player.position);
	const positionIndex = getStatisticsPosition(player.position);
	const headlineKeys = HEADLINE_METRIC_KEYS[positionIndex] ?? [];
	const resolvedSeason = seasonLabel
		? `Temporada ${seasonLabel}`
		: "Temporada no disponible";
	const resolvedExtraction = formatDate(extractionDate);
	const resolvedBirth = formatDate(player.dateOfBirth);

	return (
		<section
			className={styles.playerOverview}
			aria-label="Identidad y estadísticas principales del jugador"
		>
			<div className={styles.playerOverview__identityBlock}>
				<PlayerPortrait
					className={styles.playerOverview__portrait}
					name={player.name}
					src={player.imageUrl}
					fallbackSrc={player.fallbackImageUrl}
					fit="contain"
				/>
				<div className={styles.playerOverview__info}>
					<h1 className={styles.playerOverview__name}>{player.name}</h1>

					<div className={styles.playerOverview__teamLine}>
						<span className={styles.playerOverview__team}>
							<TeamBadge
								teamName={player.teamName}
								leagueName={player.leagueName}
							/>
							{player.teamName}
						</span>
						<span
							className={styles.playerOverview__separator}
							aria-hidden="true"
						>
							·
						</span>
						<span className={styles.playerOverview__league}>
							{league.image && <img src={league.image} alt="" />}
							{player.leagueName}
						</span>
					</div>

					<div className={styles.playerOverview__metaLine}>
						<span className={styles.playerOverview__metaItem}>
							<Shield size={16} aria-hidden="true" />
							<span>{positionLabel}</span>
						</span>
						<span
							className={styles.playerOverview__separator}
							aria-hidden="true"
						>
							·
						</span>
						<span className={styles.playerOverview__metaItem}>
							<NationalityFlag nationality={player.nationality} />
							<span>{player.nationality ?? "No disponible"}</span>
						</span>
						<span
							className={styles.playerOverview__separator}
							aria-hidden="true"
						>
							·
						</span>
						<span className={styles.playerOverview__metaItem}>
							<span>{resolvedBirth}</span>
						</span>
					</div>
				</div>
			</div>

			<section
				className={styles.playerOverview__headlineMetrics}
				aria-label={`Estadísticas principales de ${positionLabel.toLowerCase()}`}
			>
				{headlineKeys.map((key) => {
					const metric = statistics
						? getStatisticsMetric(statistics, key)
						: { key, label: key, unit: "", value: null };
					const isPercentage = metric.unit === "%";

					return (
						<div className={styles.playerOverview__metricCard} key={metric.key}>
							<span className={styles.playerOverview__metricLabel}>
								{statistics ? metric.label : getStatisticsMetricLabel(key)}
							</span>
							<strong className={styles.playerOverview__metricValue}>
								{formatStatisticsValue(metric.value)}
								{isPercentage && metric.value !== null ? " %" : ""}
							</strong>
							<small className={styles.playerOverview__metricUnit}>
								{getHeadlineUnit(key)}
							</small>
						</div>
					);
				})}
			</section>

			<div className={styles.playerOverview__temporalContext}>
				<div className={styles.playerOverview__seasonBadge}>
					{resolvedSeason}
				</div>
				<p className={styles.playerOverview__updatedAt}>
					<span>Actualizado:</span>
					<strong>{resolvedExtraction}</strong>
				</p>
			</div>
		</section>
	);
}

function getStatisticsMetricLabel(key: string): string {
	const labels: Record<string, string> = {
		saves_per_match: "Atajadas",
		pass_completion_percentage: "Precisión de pases",
		successful_long_balls_per_match: "Pases largos acertados",
		tackles_per_match: "Entradas",
		interceptions_per_match: "Intercepciones",
		clearances_per_match: "Despejes",
		passes_per_match: "Pases por partido",
		key_passes_per_match: "Pases clave",
		goals_total: "Goles",
		shots_per_match: "Tiros por partido",
		assists_total: "Asistencias",
	};
	return labels[key] ?? key;
}

function getHeadlineUnit(key: string): string {
	if (key === "pass_completion_percentage") return "% acierto";
	if (key.endsWith("_total")) return "total temporada";
	return "por partido";
}
