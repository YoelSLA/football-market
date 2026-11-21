import type { EnrichedUserOrder } from "@/features/market/hooks";
import {
	formatCreditsAmount,
	formatOrderRelativeExpiration,
} from "@/features/market/utils";
import { PlayerPortrait, TeamBadge } from "@/shared/components";
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
				<div className={styles.ordersTable__orderTicket}>
					#{order.listingId}
				</div>
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
						<div className={styles.ordersTable__playerNameRow}>
							<span className={styles.ordersTable__playerName}>
								{order.playerName}
							</span>
							{order.position && (
								<span
									className={styles.ordersTable__positionBadge}
									title={`Posición: ${order.position}`}
								>
									{order.position.slice(0, 3)}
								</span>
							)}
						</div>
						<div className={styles.ordersTable__playerMeta}>
							{order.teamName ? (
								<span className={styles.ordersTable__playerAffiliation}>
									<TeamBadge
										teamName={order.teamName}
										leagueName={order.leagueName ?? undefined}
										className={styles.ordersTable__teamBadge}
									/>
									<span>{order.teamName}</span>
								</span>
							) : (
								<span>Sin club</span>
							)}
							{order.leagueName && (
								<>
									<span className={styles.ordersTable__metaSep}>·</span>
									<span>{order.leagueName}</span>
								</>
							)}
						</div>
					</div>
				</div>
			</td>

			<td className={styles.ordersTable__colQuantity}>
				<div className={styles.ordersTable__quantityValue}>
					{order.quantity}
				</div>
				<div className={styles.ordersTable__quantityUnit}>
					{order.quantity === 1 ? "token" : "tokens"}
				</div>
			</td>

			<td className={styles.ordersTable__colPrice}>
				<div className={styles.ordersTable__priceValue}>
					{formatCreditsAmount(order.unitPrice, true)}
				</div>
				<div className={styles.ordersTable__priceUnit}>/ token</div>
			</td>

			<td className={styles.ordersTable__colTotal}>
				<div
					className={`${styles.ordersTable__totalValue} ${
						order.status === "CANCELLED" || order.status === "EXPIRED"
							? styles.ordersTable__totalValue_inactive
							: styles.ordersTable__totalValue_active
					}`}
				>
					{formatCreditsAmount(order.totalAmount, true)}
				</div>
				<div className={styles.ordersTable__totalUnit}>₵</div>
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
