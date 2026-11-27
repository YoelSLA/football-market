import type { ReactNode } from "react";
import type { EnrichedUserOrder } from "@/features/market/hooks";
import MarketOrdersRow from "./MarketOrdersRow";
import styles from "./MarketOrdersTable.module.scss";

interface Props {
	toolbar?: ReactNode;
	isLoading?: boolean;
	errorMessage?: string | null;
	orders: EnrichedUserOrder[];
	emptyState?: ReactNode;
	onCancel: (order: EnrichedUserOrder) => void;
	onRetry?: () => void;
}

export default function MarketOrdersTable({
	toolbar,
	isLoading,
	errorMessage,
	orders,
	emptyState,
	onCancel,
	onRetry,
}: Props) {
	if (errorMessage) {
		return (
			<div className={styles.ordersTable}>
				{toolbar && (
					<div className={styles.ordersTable__toolbarWrapper}>{toolbar}</div>
				)}
				<div className={styles.ordersTable__state}>
					<p className={styles.ordersTable__errorMessage}>{errorMessage}</p>
					{onRetry && (
						<button
							type="button"
							onClick={onRetry}
							className={styles.ordersTable__retryBtn}
						>
							Reintentar consulta
						</button>
					)}
				</div>
			</div>
		);
	}

	if (isLoading) {
		return (
			<div className={styles.ordersTable}>
				{toolbar && (
					<div className={styles.ordersTable__toolbarWrapper}>{toolbar}</div>
				)}
				<div className={styles.ordersTable__state} aria-busy="true">
					<p role="status" className={styles.ordersTable__loadingText}>
						Cargando tus órdenes de venta…
					</p>
				</div>
			</div>
		);
	}

	if (orders.length === 0) {
		return (
			<div className={styles.ordersTable}>
				{toolbar && (
					<div className={styles.ordersTable__toolbarWrapper}>{toolbar}</div>
				)}
				{emptyState}
			</div>
		);
	}

	return (
		<div className={styles.ordersTable}>
			{toolbar && (
				<div className={styles.ordersTable__toolbarWrapper}>{toolbar}</div>
			)}
			<div className={styles.ordersTable__container}>
				<table className={styles.ordersTable__grid}>
					<thead>
						<tr>
							<th scope="col" className={styles.ordersTable__colOrder}>
								Orden / Registro
							</th>
							<th scope="col" className={styles.ordersTable__colPlayer}>
								Jugador
							</th>
							<th scope="col" className={styles.ordersTable__colQuantity}>
								Cantidad
							</th>
							<th scope="col" className={styles.ordersTable__colPrice}>
								Precio Unit.
							</th>
							<th scope="col" className={styles.ordersTable__colTotal}>
								Total Importe
							</th>
							<th scope="col" className={styles.ordersTable__colStatus}>
								Estado
							</th>
							<th scope="col" className={styles.ordersTable__colAction}>
								Acción
							</th>
						</tr>
					</thead>
					<tbody>
						{orders.map((order) => (
							<MarketOrdersRow
								key={order.listingId}
								order={order}
								onCancel={onCancel}
							/>
						))}
					</tbody>
				</table>
			</div>
		</div>
	);
}
