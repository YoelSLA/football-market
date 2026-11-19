import type { EnrichedUserOrder } from "@/features/market/hooks";
import { Pagination } from "@/shared/components";
import MarketOrdersRow from "./MarketOrdersRow";
import styles from "./MarketOrdersTable.module.scss";

interface Props {
	orders: EnrichedUserOrder[];
	page: number;
	totalPages: number;
	totalElements: number;
	onPageChange: (page: number) => void;
	onCancel: (order: EnrichedUserOrder) => void;
}

export default function MarketOrdersTable({
	orders,
	page,
	totalPages,
	totalElements,
	onPageChange,
	onCancel,
}: Props) {
	const fromIndex = totalElements === 0 ? 0 : page * 10 + 1;
	const toIndex = Math.min((page + 1) * 10, totalElements);

	return (
		<div className={styles.ordersTable}>
			<div className={styles.ordersTable__container}>
				<table className={styles.ordersTable__grid}>
					<thead>
						<tr>
							<th className={styles.ordersTable__colOrder}>Orden / Fecha</th>
							<th className={styles.ordersTable__colPlayer}>Jugador</th>
							<th className={styles.ordersTable__colQuantity}>Cantidad</th>
							<th className={styles.ordersTable__colPrice}>Precio Unit.</th>
							<th className={styles.ordersTable__colTotal}>Total Importe</th>
							<th className={styles.ordersTable__colStatus}>Estado</th>
							<th className={styles.ordersTable__colAction}>Acción</th>
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

			<div className={styles.ordersTable__footer}>
				<span className={styles.ordersTable__footerText}>
					Mostrando {fromIndex}–{toIndex} de {totalElements} órdenes
				</span>

				{totalPages > 1 && (
					<Pagination
						page={page}
						totalPages={totalPages}
						onChange={onPageChange}
					/>
				)}
			</div>
		</div>
	);
}
