import { DEMO_DATA_LABEL, DEMO_SEASON } from "../../constants";
import type { PlayerSampleSeason } from "../../types";
import styles from "./PlayerSportsSummary.module.scss";

interface PlayerSportsSummaryProps {
	sampleSeason?: PlayerSampleSeason;
	className?: string;
}

/**
 * Tarjeta compacta que ocupa el lugar del radar: partidos, goles, asistencias y minutos
 * de la temporada fija. No dibuja radar, percentiles, barras normalizadas ni métricas
 * adicionales, y siempre identifica su procedencia demostrativa.
 */
export function PlayerSportsSummary({
	sampleSeason,
	className,
}: PlayerSportsSummaryProps) {
	const items = sampleSeason
		? [
				{ label: "Partidos", value: sampleSeason.matches },
				{ label: "Goles", value: sampleSeason.goals },
				{ label: "Asistencias", value: sampleSeason.assists },
				{ label: "Minutos", value: sampleSeason.minutesPlayed },
			]
		: [];

	return (
		<section
			className={`${styles["player-sports-summary"]} ${className ?? ""}`.trim()}
			aria-label="Rendimiento deportivo"
		>
			<header>
				<h2>Rendimiento deportivo</h2>
				<span className={styles["player-sports-summary__season"]}>
					{DEMO_SEASON}
				</span>
			</header>
			<dl>
				{items.map((item) => (
					<div key={item.label}>
						<dt>{item.label}</dt>
						<dd>{item.value}</dd>
					</div>
				))}
			</dl>
			<p className={styles["player-sports-summary__demo"]}>{DEMO_DATA_LABEL}</p>
		</section>
	);
}
