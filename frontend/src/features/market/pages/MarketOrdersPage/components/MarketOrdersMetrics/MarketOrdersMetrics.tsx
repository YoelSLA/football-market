import type { UserOrdersSummary } from "@/features/market/types";
import { formatCreditsAmount } from "@/features/market/utils";
import styles from "./MarketOrdersMetrics.module.scss";

interface Props {
	summary: UserOrdersSummary;
	isLoading?: boolean;
}

export default function MarketOrdersMetrics({ summary, isLoading }: Props) {
	if (isLoading) {
		return (
			<section
				className={styles.ordersMetrics}
				aria-label="Cargando métricas de órdenes"
			>
				{[1, 2, 3, 4].map((i) => (
					<div key={i} className={styles.ordersMetrics__card}>
						<span className={styles.ordersMetrics__label}>Cargando...</span>
						<span className={styles.ordersMetrics__value}>—</span>
						<span className={styles.ordersMetrics__subvalue}>—</span>
					</div>
				))}
			</section>
		);
	}

	const closedCount = summary.cancelledOrders + summary.expiredOrders;

	return (
		<section
			className={styles.ordersMetrics}
			aria-label="Resumen de órdenes del usuario"
		>
			<div className={styles.ordersMetrics__card}>
				<span className={styles.ordersMetrics__label}>Órdenes Activas</span>
				<span className={styles.ordersMetrics__value}>
					{summary.activeOrders}{" "}
					{summary.activeOrders === 1 ? "activa" : "activas"}
				</span>
				<span className={styles.ordersMetrics__subvalue}>
					{summary.activeTokens} tokens en venta
				</span>
			</div>

			<div className={styles.ordersMetrics__card}>
				<span className={styles.ordersMetrics__label}>Valor en Venta</span>
				<span className={styles.ordersMetrics__value}>
					{formatCreditsAmount(summary.activeValue)}
				</span>
				<span className={styles.ordersMetrics__subvalue}>
					Importe en oferta
				</span>
			</div>

			<div className={styles.ordersMetrics__card}>
				<span className={styles.ordersMetrics__label}>Órdenes Vendidas</span>
				<span className={styles.ordersMetrics__value}>
					{summary.soldOrders}{" "}
					{summary.soldOrders === 1 ? "vendida" : "vendidas"}
				</span>
				<span className={styles.ordersMetrics__subvalue}>
					{summary.soldTokens} tokens liquidados
				</span>
			</div>

			<div className={styles.ordersMetrics__card}>
				<span className={styles.ordersMetrics__label}>
					Cerradas / Expiradas
				</span>
				<span className={styles.ordersMetrics__value}>
					{closedCount} {closedCount === 1 ? "cerrada" : "cerradas"}
				</span>
				<span className={styles.ordersMetrics__subvalue}>
					{summary.cancelledOrders} canceladas · {summary.expiredOrders}{" "}
					expiradas
				</span>
			</div>
		</section>
	);
}
