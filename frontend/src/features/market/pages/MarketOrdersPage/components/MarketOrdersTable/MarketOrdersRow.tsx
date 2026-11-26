import type { EnrichedUserOrder } from "@/features/market/hooks";
import {
	formatCreditsAmount,
	formatOrderRelativeExpiration,
} from "@/features/market/utils";
import { PlayerTableCell, TokenSymbol } from "@/shared/components";
import { CircleX } from "@/shared/icons";
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
	hour12: false,
});

function formatDate(iso: string | null | undefined): string {
	if (!iso) return "—";
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
		<tr className={styles.ordersTable__row}>
			<td className={styles.ordersTable__colOrder}>
				<span className={styles.ordersTable__ticketBadge}>
					#{order.listingId}
				</span>
				<div className={styles.ordersTable__orderDate}>
					{formatDate(order.createdAt)}
				</div>
			</td>

			<td className={styles.ordersTable__colPlayer}>
				<PlayerTableCell
					player={{
						id: order.playerId,
						name: order.playerName,
						imageUrl: order.imageUrl,
						fallbackImageUrl: order.fallbackImageUrl,
						position: order.position,
						teamId: order.teamId,
						teamName: order.teamName,
						leagueId: order.leagueId,
						leagueName: order.leagueName,
					}}
					href={`/players/${order.playerId}`}
				/>
			</td>

			<td className={styles.ordersTable__colQuantity}>
				<span className={styles.ordersTable__quantityValue}>
					{order.quantity} <TokenSymbol />
				</span>
			</td>

			<td className={styles.ordersTable__colPrice}>
				<span className={styles.ordersTable__priceValue}>
					{formatCreditsAmount(order.unitPrice, true)} /{" "}
					<TokenSymbol size="sm" />
				</span>
			</td>

			<td className={styles.ordersTable__colTotal}>
				<strong
					className={`${styles.ordersTable__totalValue} ${
						order.status === "CANCELLED" || order.status === "EXPIRED"
							? styles.ordersTable__totalValue_inactive
							: styles.ordersTable__totalValue_active
					}`}
				>
					{formatCreditsAmount(order.totalAmount, true)}
				</strong>
			</td>

			<td className={styles.ordersTable__colStatus}>
				<div className={styles.ordersTable__statusCell}>
					<span
						className={`${styles.ordersTable__badge} ${
							styles[`ordersTable__badge_${order.status}`]
						}`}
					>
						<span className={styles.ordersTable__badgeDot} aria-hidden="true" />
						{statusLabels[order.status]}
					</span>
					<span className={styles.ordersTable__statusMeta}>
						{formatOrderRelativeExpiration(order.expiresAt, order.status)}
					</span>
				</div>
			</td>

			<td className={styles.ordersTable__colAction}>
				{order.status === "ACTIVE" ? (
					<button
						type="button"
						onClick={() => onCancel(order)}
						className={styles.ordersTable__cancelBtn}
						aria-label={`Cancelar orden #${order.listingId} de ${order.playerName}`}
					>
						<CircleX size={13} aria-hidden="true" />
						<span>Cancelar</span>
					</button>
				) : (
					<span className={styles.ordersTable__actionEmpty} aria-hidden="true">
						—
					</span>
				)}
			</td>
		</tr>
	);
}
