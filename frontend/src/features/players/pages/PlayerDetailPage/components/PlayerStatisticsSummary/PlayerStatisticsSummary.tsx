import type { PlayerStatistics } from "@/features/players/types";
import {
	getStatisticsPositionLabel,
	getSummaryPositionProfile,
	resolveSummaryMetric,
} from "@/features/players/utils";
import SummaryKpiCard from "./components/SummaryKpiCard";
import SummaryPanel from "./components/SummaryPanel";
import styles from "./PlayerStatisticsSummary.module.scss";

interface Props {
	statistics: PlayerStatistics;
	position: string;
}

export default function PlayerStatisticsSummary({
	statistics,
	position,
}: Props) {
	const profile = getSummaryPositionProfile(position);
	const positionLabel = getStatisticsPositionLabel(position);

	if (!profile) {
		return (
			<section
				className={styles.playerStatisticsSummary}
				aria-label={`Resumen estadístico como ${positionLabel.toLowerCase()}`}
			>
				<p className={styles.playerStatisticsSummary__fallback} role="status">
					No hay un perfil estadístico definido para esta posición. Se muestran
					los datos generales en el catálogo de estadísticas.
				</p>
			</section>
		);
	}

	const resolvedKpis = profile.kpis.map((slot) =>
		resolveSummaryMetric(statistics, slot),
	);

	const resolvedPanels = profile.panels.map((panel) => ({
		...panel,
		resolvedMetrics: panel.metrics.map((slot) =>
			resolveSummaryMetric(statistics, slot),
		),
	}));

	return (
		<section
			className={styles.playerStatisticsSummary}
			aria-label={`Resumen deportivo como ${positionLabel.toLowerCase()}`}
		>
			<div
				className={styles.playerStatisticsSummary__cards}
				style={
					{
						"--kpi-columns": resolvedKpis.length,
					} as React.CSSProperties
				}
			>
				{resolvedKpis.map((metric) => (
					<SummaryKpiCard key={metric.key} metric={metric} />
				))}
			</div>

			<div className={styles.playerStatisticsSummary__panels}>
				{resolvedPanels.map((panel) => (
					<SummaryPanel
						key={panel.title}
						title={panel.title}
						subtitle={panel.subtitle}
						icon={panel.icon}
						metrics={panel.resolvedMetrics}
					/>
				))}
			</div>
		</section>
	);
}
