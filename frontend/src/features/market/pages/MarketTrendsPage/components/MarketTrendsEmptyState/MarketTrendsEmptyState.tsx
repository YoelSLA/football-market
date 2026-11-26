import { Info } from "@/shared/icons";
import styles from "./MarketTrendsEmptyState.module.scss";

export default function MarketTrendsEmptyState() {
	return (
		<div className={styles.container}>
			<div className={styles.iconTile}>
				<Info className={styles.icon} aria-hidden="true" />
			</div>
			<h3 className={styles.title}>Mercado en calma</h3>
			<p className={styles.description}>
				No se registraron transacciones de compraventa en las últimas 24 horas.
				El sentimiento, la cuota de demanda y los activos en tendencia se
				calcularán automáticamente en cuanto los usuarios comiencen a operar en
				el mercado oficial o P2P.
			</p>
		</div>
	);
}
