import type { ReactNode } from "react";
import type { RankingMetadata } from "@/features/players/types";
import { BarChart3, CalendarDays, Trophy, UsersRound } from "@/shared/icons";
import styles from "./RankingsHeader.module.scss";

interface Props {
	metadata: RankingMetadata;
	isLoading?: boolean;
}

const VARIANT_CLASSES = {
	blue: styles.header__metric_blue,
	violet: styles.header__metric_violet,
	indigo: styles.header__metric_indigo,
} as const;

export default function RankingsHeader({ metadata, isLoading }: Props) {
	const metrics: Array<{
		label: string;
		value: ReactNode;
		detail: ReactNode;
		icon: typeof Trophy;
		variant: "blue" | "violet" | "indigo";
	}> = [
		{
			label: "Estrategia activa",
			value: metadata.strategyLabel,
			detail: "Modelo deportivo",
			icon: BarChart3,
			variant: "blue",
		},
		{
			label: "Fecha de valuación",
			value: metadata.referenceDate ?? "—",
			detail: "Cierre de mercado",
			icon: CalendarDays,
			variant: "violet",
		},
		{
			label: "Clasificados",
			value: isLoading
				? "—"
				: metadata.totalClassified !== null
					? `${metadata.totalClassified.toLocaleString("es-AR")}`
					: "—",
			detail: "Futbolistas evaluados",
			icon: UsersRound,
			variant: "indigo",
		},
	];

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
						Clasificación oficial de rendimiento deportivo y cotización en el
						mercado.
					</p>
				</div>
			</div>

			<div className={styles.header__metrics}>
				{metrics.map((item) => {
					const Icon = item.icon;
					return (
						<div
							key={item.label}
							className={`${styles.header__metricItem} ${
								VARIANT_CLASSES[item.variant]
							}`}
						>
							<div className={styles.header__metricIconWrapper}>
								<Icon
									className={styles.header__metricIcon}
									aria-hidden="true"
								/>
							</div>
							<div className={styles.header__metricBody}>
								<span className={styles.header__metricLabel}>{item.label}</span>
								<strong className={styles.header__metricValue}>
									{item.value}
								</strong>
								<span className={styles.header__metricDetail}>
									{item.detail}
								</span>
							</div>
						</div>
					);
				})}
			</div>
		</header>
	);
}
