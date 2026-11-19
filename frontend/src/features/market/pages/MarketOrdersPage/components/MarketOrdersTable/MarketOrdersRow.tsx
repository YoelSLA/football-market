import type { EnrichedUserOrder } from "@/features/market/hooks";
import { formatCreditsAmount } from "@/features/market/utils";
import { PlayerPortrait } from "@/shared/components";
import styles from "./MarketOrdersTable.module.scss";

interface Props {
	order: EnrichedUserOrder;
	onCancel: (order: EnrichedUserOrder) => void;
}

const dateFormatter = new Intl.DateTimeFormat("es-AR", {
	day: "2-digit",
	month: "2-digit",
	year: "numeric",
	hour: "2-digit",
	minute: "2-digit",
});

function formatDate(iso: string): string {
	try {
		const d = new Date(iso);
		if (!Number.isNaN(d.getTime())) return dateFormatter.format(d);
	} catch {
		// fallback
	}
	return iso;
}

const statusLabels: Record<EnrichedUserOrder["status"], string> = {
	ACTIVE: "Activa",
	SOLD: "Vendida",
	CANCELLED: "Cancelada",
	EXPIRED: "Expirada",
};

export default function MarketOrdersRow({ order, onCancel }: Props) {
	return (
		<tr>
			<td className={styles.ordersTable__colOrder}>
				<div className={styles.ordersTable__orderId}>#{order.listingId}</div>
				<div className={styles.ordersTable__orderDate}>
					{formatDate(order.createdAt)}
				</div>
			</td>

			<td className={styles.ordersTable__colPlayer}>
				<div className={styles.ordersTable__playerCell}>
					<PlayerPortrait
						src={order.imageUrl}
						name={order.playerName}
						size="sm"
					/>
					<div className={styles.ordersTable__playerInfo}>
						<span className={styles.ordersTable__playerName}>
							{order.playerName}
						</span>
						<span className={styles.ordersTable__playerMeta}>
							{order.teamName ?? "Sin club"}
							{order.leagueName ? ` · ${order.leagueName}` : ""}
						</span>
					</div>
				</div>
			</td>

			<td className={styles.ordersTable__colQuantity}>
				{order.quantity} {order.quantity === 1 ? "token" : "tokens"}
			</td>

			<td className={styles.ordersTable__colPrice}>
				{formatCreditsAmount(order.unitPrice)}
			</td>

			<td className={styles.ordersTable__colTotal}>
				{formatCreditsAmount(order.totalAmount)}
			</td>

			<td className={styles.ordersTable__colStatus}>
				<span
					className={`${styles.ordersTable__badge} ${
						styles[`ordersTable__badge_${order.status}`]
					}`}
				>
					{statusLabels[order.status]}
				</span>
			</td>

			<td className={styles.ordersTable__colAction}>
				{order.status === "ACTIVE" ? (
					<button
						type="button"
						onClick={() => onCancel(order)}
						className={styles.ordersTable__cancelBtn}
						aria-label={`Cancelar orden #${order.listingId} de ${order.playerName}`}
					>
						Cancelar
					</button>
				) : (
					<span aria-hidden="true">—</span>
				)}
			</td>
		</tr>
	);
}
