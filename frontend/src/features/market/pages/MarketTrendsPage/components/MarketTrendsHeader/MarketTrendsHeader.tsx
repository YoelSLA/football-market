import { Button } from "@/shared/components";
import { Clock, RotateCw, TrendingUp } from "@/shared/icons";
import styles from "./MarketTrendsHeader.module.scss";

export interface MarketTrendsHeaderProps {
	isFetching: boolean;
	onRefresh: () => void;
}

export default function MarketTrendsHeader({
	isFetching,
	onRefresh,
}: MarketTrendsHeaderProps) {
	const windowLabel = "Ventana: Últimas 24 horas";

	return (
		<div className={styles.header}>
			<div className={styles.header__left}>
				<TrendingUp className={styles.header__icon} aria-hidden="true" />
				<h1 className={styles.header__title}>Tendencias del Mercado</h1>
				<span className={styles.header__tag}>En tiempo real</span>
			</div>

			<div className={styles.header__right}>
				<span className={styles.header__badge} title="Período de análisis">
					<Clock className={styles.header__badgeIcon} aria-hidden="true" />
					{windowLabel}
				</span>

				<Button
					unstyled
					type="button"
					onClick={onRefresh}
					disabled={isFetching}
					aria-label="Actualizar datos de tendencias"
					className={styles.header__refreshButton}
				>
					<RotateCw
						size={13}
						className={isFetching ? styles.header__spinning : undefined}
						aria-hidden="true"
					/>
					<span>{isFetching ? "Actualizando…" : "Actualizar"}</span>
				</Button>
			</div>
		</div>
	);
}
