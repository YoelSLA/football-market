import type { EnrichedUserOrder } from "@/features/market/hooks";
import { formatCreditsAmount } from "@/features/market/utils";
import { TOKEN_SYMBOL } from "@/shared/constants";
import styles from "./CancelListingDetails.module.scss";

interface Props {
	order: EnrichedUserOrder;
}

export function CancelListingDetails({ order }: Props) {
	return (
		<ul className={styles.details}>
			<li>
				<span>Jugador:</span>
				<strong>
					{order.playerName}
					{order.teamName ? ` (${order.teamName})` : ""}
				</strong>
			</li>
			<li>
				<span>Lote publicado:</span>
				<strong>
					{order.quantity} {TOKEN_SYMBOL}
				</strong>
			</li>
			<li>
				<span>Precio unitario:</span>
				<strong>{formatCreditsAmount(order.unitPrice)}</strong>
			</li>
			<li>
				<span>Total del lote:</span>
				<strong>{formatCreditsAmount(order.totalAmount)}</strong>
			</li>
		</ul>
	);
}
