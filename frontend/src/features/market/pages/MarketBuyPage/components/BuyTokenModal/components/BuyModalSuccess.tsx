import type { BuyResult, OfficialMarketPlayer } from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { TOKEN_SYMBOL } from "@/shared/constants";
import { CheckCircle2 } from "@/shared/icons";
import styles from "./BuyModalSuccess.module.scss";

interface Props {
	player: OfficialMarketPlayer;
	purchaseResult: BuyResult;
}

const dateTimeFormatter = new Intl.DateTimeFormat("es-AR", {
	dateStyle: "short",
	timeStyle: "short",
});

export function BuyModalSuccess({ player, purchaseResult }: Props) {
	return (
		<div role="status" className={styles.successCard}>
			<div className={styles.successHeader}>
				<CheckCircle2 aria-hidden="true" />
				<h4>Compra confirmada</h4>
			</div>
			<p className={styles.successSub}>
				Transacción #{purchaseResult.transactionId} registrada exitosamente
			</p>
			<div className={styles.successDetails}>
				<div className={styles.detailRow}>
					<span>Jugador</span>
					<strong>{player.name}</strong>
				</div>
				<div className={styles.detailRow}>
					<span>Tokens adquiridos</span>
					<strong>
						{purchaseResult.quantity} {TOKEN_SYMBOL}
					</strong>
				</div>
				<div className={styles.detailRow}>
					<span>Precio unitario</span>
					<span>{formatCredits(purchaseResult.unitPrice)}</span>
				</div>
				<div className={styles.detailDivider} />
				<div className={styles.detailRow}>
					<span>Total abonado</span>
					<strong className={styles.successTotal}>
						{formatCredits(purchaseResult.totalAmount)}
					</strong>
				</div>
				<div className={styles.detailRow}>
					<span>Fecha y hora</span>
					<span>
						{dateTimeFormatter.format(new Date(purchaseResult.executedAt))}
					</span>
				</div>
			</div>
		</div>
	);
}
