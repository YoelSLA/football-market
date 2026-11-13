import type { RankingMetadata, RankingMode } from "@/features/players/types";
import { BarChart3, Coins, Info, Trophy } from "@/shared/icons";
import styles from "./RankingsHeader.module.scss";

interface Props {
	mode: RankingMode;
	onModeChange: (mode: RankingMode) => void;
	metadata: RankingMetadata;
}

export default function RankingsHeader({
	mode,
	onModeChange,
	metadata,
}: Props) {
	return (
		<header className={styles.header}>
			<div className={styles.header__left}>
				<div className={styles.header__iconBox}>
					<Trophy
						size={26}
						className={styles.header__trophy}
						aria-hidden="true"
					/>
				</div>
				<div className={styles.header__titleGroup}>
					<h1 className={styles.header__title}>Ranking de jugadores</h1>
					<p className={styles.header__subtitle}>
						{mode === "PERFORMANCE"
							? "Clasificación oficial según el modelo de rendimiento y valuación de Football Market."
							: "Clasificación oficial según la cotización vigente de los tokens en el mercado."}
					</p>
				</div>
			</div>

			<div className={styles.header__right}>
				<div className={styles.header__toggle}>
					<button
						type="button"
						aria-pressed={mode === "PERFORMANCE"}
						className={`${styles.toggleButton} ${
							mode === "PERFORMANCE" ? styles.toggleButton_active : ""
						}`}
						onClick={() => onModeChange("PERFORMANCE")}
					>
						<BarChart3 size={16} aria-hidden="true" />
						<span>Por rendimiento</span>
					</button>
					<button
						type="button"
						aria-pressed={mode === "VALUATION"}
						className={`${styles.toggleButton} ${
							mode === "VALUATION" ? styles.toggleButton_active : ""
						}`}
						onClick={() => onModeChange("VALUATION")}
					>
						<Coins size={16} aria-hidden="true" />
						<span>Por cotización</span>
					</button>
				</div>

				<div className={styles.metricsBar}>
					{mode === "PERFORMANCE" ? (
						<>
							<div className={styles.metricColumn}>
								<span className={styles.metricLabel}>Estrategia activa</span>
								<strong className={styles.metricValue}>
									{metadata.strategyLabel}
								</strong>
							</div>
							<div className={styles.metricDivider} />
							<div className={styles.metricColumn}>
								<span className={styles.metricLabel}>Fecha de valuación</span>
								<strong className={styles.metricValue}>
									{metadata.referenceDate ?? "—"}
								</strong>
							</div>
							<div className={styles.metricDivider} />
							<div className={styles.metricColumn}>
								<span className={styles.metricLabel}>
									Jugadores clasificados
								</span>
								<strong className={styles.metricValue}>
									{metadata.totalClassified !== null
										? metadata.totalClassified.toLocaleString("es-AR")
										: "—"}
								</strong>
							</div>
						</>
					) : (
						<>
							<div className={styles.metricColumn}>
								<span className={styles.metricLabel}>Última actualización</span>
								<strong className={styles.metricValue}>
									{metadata.referenceDate ?? "—"}
								</strong>
							</div>
							<div className={styles.metricDivider} />
							<div className={styles.metricColumn}>
								<span className={styles.metricLabel}>
									Jugadores clasificados
								</span>
								<strong className={styles.metricValue}>
									{metadata.totalClassified !== null
										? metadata.totalClassified.toLocaleString("es-AR")
										: "—"}
								</strong>
							</div>
							<div className={styles.metricDivider} />
							<div className={styles.metricColumn}>
								<span className={styles.metricLabel}>
									Origen de cotizaciones{" "}
									<Info
										size={12}
										className={styles.infoIcon}
										aria-hidden="true"
									/>
								</span>
								<strong className={styles.metricValue}>Mercado</strong>
							</div>
						</>
					)}
				</div>
			</div>
		</header>
	);
}
