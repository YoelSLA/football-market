import type { PlayerStatistics } from "@/features/players/types";
import {
	formatStatisticsValue,
	getHighlightedStatistics,
	getStatisticsMetric,
	getStatisticsPanels,
	getStatisticsPositionLabel,
	getStatisticsProfile,
} from "@/features/players/utils";
import {
	Activity,
	BarChart3,
	CircleDot,
	Footprints,
	Goal,
	Shield,
	Star,
} from "@/shared/icons";
import PlayerStatisticsBars, { type BarItem } from "../PlayerStatisticsBars";
import styles from "./PlayerStatisticsSummary.module.scss";

interface Props {
	statistics: PlayerStatistics;
	position: string;
}

function getCardSubtitle(key: string, unit: string): string {
	if (key === "whoscored_rating_average") return "promedio WhoScored";
	if (key === "pass_completion_percentage") return "pases acertados";
	if (key === "shots_on_target_total") return "tiros temporada";
	if (key.includes("aerial_duels_won")) return "ganados/partido";
	if (key.endsWith("_per_match")) return "por partido";
	if (key.endsWith("_total")) return "total temporada";
	return unit || "por partido";
}

function getCardIcon(key: string, index: number) {
	if (key === "whoscored_rating_average") return Star;
	if (
		key.includes("aerial") ||
		key.includes("tackle") ||
		key.includes("clearance") ||
		key.includes("block")
	)
		return Shield;
	if (key.includes("pass") || key.includes("assist")) return CircleDot;
	if (key.includes("goal") || key.includes("shot")) return Goal;
	if (key.includes("dribble")) return Footprints;
	const fallbackIcons = [Star, Shield, CircleDot, Footprints];
	return fallbackIcons[index % fallbackIcons.length];
}

export default function PlayerStatisticsSummary({
	statistics,
	position,
}: Props) {
	const profile = getStatisticsProfile(position);
	const positionLabel = getStatisticsPositionLabel(position);
	const panels = getStatisticsPanels(position);
	const cardKeys = profile?.cards ?? [
		"whoscored_rating_average",
		"appearances_total",
		"minutes_played_total",
	];
	const highlightedMetrics = getHighlightedStatistics(statistics, position);

	return (
		<section
			className={styles.playerStatisticsSummary}
			aria-label={`Resumen estadístico como ${positionLabel.toLowerCase()}`}
		>
			<header className={styles.playerStatisticsSummary__header}>
				<h2 className={styles.playerStatisticsSummary__title}>
					Rendimiento como {positionLabel}
				</h2>
				<p className={styles.playerStatisticsSummary__subtitle}>
					Datos de temporada · Los valores por partido no se convierten en
					totales
				</p>
			</header>

			{!profile && (
				<p className={styles.playerStatisticsSummary__fallback} role="status">
					No hay un perfil estadístico definido para esta posición. Se muestran
					datos generales.
				</p>
			)}

			<div className={styles.playerStatisticsSummary__cards}>
				{cardKeys.map((key, index) => {
					const item = getStatisticsMetric(statistics, key);
					const Icon = getCardIcon(key, index);
					const isRating = key === "whoscored_rating_average";
					const isPercentage = item.unit === "%";
					const title = isRating ? "Calificación" : item.label;
					const subtitle = getCardSubtitle(key, item.unit);

					return (
						<article key={key} className={styles.playerStatisticsSummary__card}>
							<span className={styles.playerStatisticsSummary__icon}>
								<Icon size={26} aria-hidden="true" />
							</span>
							<div className={styles.playerStatisticsSummary__cardContent}>
								<h3>{title}</h3>
								<strong>
									{formatStatisticsValue(item.value)}
									{isPercentage && item.value !== null ? " %" : ""}
								</strong>
								<small>{subtitle}</small>
							</div>
						</article>
					);
				})}
			</div>

			<div className={styles.playerStatisticsSummary__panels}>
				{panels.map((panel) => {
					const bars: BarItem[] = panel.keys.map((key) => {
						const item = getStatisticsMetric(statistics, key);
						return {
							key: item.key,
							label: item.label,
							value: item.value,
							unit: item.unit,
						};
					});
					const hasMissingData = bars.some((bar) => bar.value === null);

					return (
						<section
							key={panel.title}
							className={styles.playerStatisticsSummary__panel}
							aria-label={panel.title}
						>
							<div className={styles.playerStatisticsSummary__panelHeader}>
								<h3>
									<BarChart3 size={16} aria-hidden="true" />
									{panel.title}
								</h3>
								<small>Valores promedio por partido</small>
							</div>
							<PlayerStatisticsBars bars={bars} />
							{hasMissingData && (
								<p className={styles.playerStatisticsSummary__note}>
									Datos parciales: las métricas ausentes no se representan como
									cero.
								</p>
							)}
						</section>
					);
				})}
			</div>

			{highlightedMetrics.length > 0 && (
				<section
					className={styles.playerStatisticsSummary__highlighted}
					aria-label="Estadísticas destacadas"
				>
					<div className={styles.playerStatisticsSummary__panelHeader}>
						<h3>
							<Activity size={16} aria-hidden="true" />
							Estadísticas destacadas
						</h3>
						<small>
							Valores originales y unidades indicadas; sin puntuación
							comparativa
						</small>
					</div>
					<dl className={styles.playerStatisticsSummary__highlightedGrid}>
						{highlightedMetrics.map((item) => (
							<div
								key={item.key}
								className={styles.playerStatisticsSummary__highlightItem}
							>
								<dt>
									<CircleDot size={13} aria-hidden="true" />
									<span>{item.label}</span>
								</dt>
								<dd>
									<strong>{formatStatisticsValue(item.value)}</strong>
									{item.unit && item.value !== null && (
										<span>{item.unit === "%" ? " %" : ` ${item.unit}`}</span>
									)}
								</dd>
							</div>
						))}
					</dl>
				</section>
			)}
		</section>
	);
}
