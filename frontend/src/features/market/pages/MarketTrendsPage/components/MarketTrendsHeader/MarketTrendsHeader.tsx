import { Button } from "@/shared/components";
import { Clock, RotateCw, TrendingUp } from "@/shared/icons";
import styles from "./MarketTrendsHeader.module.scss";

export interface MarketTrendsHeaderProps {
	startsAt?: string;
	endsAt?: string;
	isFetching: boolean;
	onRefresh: () => void;
}

function formatWindowTime(isoDate?: string): string {
	if (!isoDate) return "";
	try {
		const date = new Date(isoDate);
		return date.toLocaleTimeString("es-AR", {
			hour: "2-digit",
			minute: "2-digit",
			hour12: false,
		});
	} catch {
		return "";
	}
}

export default function MarketTrendsHeader({
	startsAt,
	endsAt,
	isFetching,
	onRefresh,
}: MarketTrendsHeaderProps) {
	const startTime = formatWindowTime(startsAt);
	const endTime = formatWindowTime(endsAt);
	const windowLabel =
		startTime && endTime
			? `Ventana: 24h (${startTime} - ${endTime})`
			: "Ventana: Últimas 24 horas";

	return (
		<header className={styles.header}>
			<div className={styles.header__main}>
				<div className={styles.header__titleGroup}>
					<div className={styles.header__iconTile}>
						<TrendingUp className={styles.header__icon} aria-hidden="true" />
					</div>
					<div>
						<h1 className={styles.header__title}>
							Tendencias y Sentimiento del Mercado
						</h1>
						<p className={styles.header__subtitle}>
							Visualización macro del comportamiento comunitario y presión
							compradora/vendedora en las últimas 24 horas.
						</p>
					</div>
				</div>
			</div>

			<div className={styles.header__actions}>
				<div className={styles.header__badge} title="Período de análisis">
					<Clock className={styles.header__badgeIcon} aria-hidden="true" />
					<span>{windowLabel}</span>
				</div>

				<Button
					variant="secondary"
					className={styles.header__refreshButton}
					onClick={onRefresh}
					disabled={isFetching}
					aria-label="Actualizar datos de tendencias"
					title="Actualizar datos de tendencias"
				>
					<RotateCw
						className={`${styles.header__refreshIcon} ${
							isFetching ? styles["header__refreshIcon--spinning"] : ""
						}`}
						aria-hidden="true"
					/>
					<span>{isFetching ? "Actualizando..." : "Actualizar"}</span>
				</Button>
			</div>
		</header>
	);
}
