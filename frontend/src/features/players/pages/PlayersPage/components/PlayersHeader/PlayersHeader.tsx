import type { ReactNode } from "react";
import type { PlayerCatalogFilters } from "@/features/players/types";
import { Shield, Trophy, UsersRound } from "@/shared/icons";
import styles from "./PlayersHeader.module.scss";

interface Props {
	totalElements?: number;
	options?: PlayerCatalogFilters;
	isLoading?: boolean;
	optionsLoading?: boolean;
}

const VARIANT_CLASSES = {
	blue: styles.header__metric_blue,
	violet: styles.header__metric_violet,
	indigo: styles.header__metric_indigo,
} as const;

export default function PlayersHeader({
	totalElements,
	options,
	isLoading,
	optionsLoading,
}: Props) {
	const metrics: Array<{
		label: string;
		value: ReactNode;
		detail: ReactNode;
		icon: typeof UsersRound;
		variant: "blue" | "violet" | "indigo";
	}> = [
		{
			label: "Total futbolistas",
			value: isLoading
				? "—"
				: totalElements !== undefined
					? `${totalElements.toLocaleString("es-AR")}`
					: "—",
			detail: "En catálogo activo",
			icon: UsersRound,
			variant: "blue",
		},
		{
			label: "Clubes disponibles",
			value: optionsLoading
				? "—"
				: options?.teams?.length
					? `${options.teams.length} clubes`
					: "—",
			detail: "Equipos federados",
			icon: Shield,
			variant: "violet",
		},
		{
			label: "Ligas oficiales",
			value: optionsLoading
				? "—"
				: options?.leagues?.length
					? `${options.leagues.length} ligas`
					: "—",
			detail: "Competiciones activas",
			icon: Trophy,
			variant: "indigo",
		},
	];

	return (
		<header className={styles.header}>
			<div className={styles.header__left}>
				<div className={styles.header__iconBox}>
					<UsersRound className={styles.header__icon} aria-hidden="true" />
				</div>
				<div className={styles.header__titleGroup}>
					<h1 className={styles.header__title}>Catálogo de jugadores</h1>
					<p className={styles.header__subtitle}>
						Explorá futbolistas, filtrá por liga y posición y consultá sus
						cotizaciones.
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
