import type { SellOfficialResult, SellResult } from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { CheckCircle2 } from "@/shared/icons";
import styles from "./SellModalSuccess.module.scss";

interface Props {
	result: SellResult | SellOfficialResult;
	playerName: string;
}

const expiryDateFormatter = new Intl.DateTimeFormat("es-AR", {
	day: "2-digit",
	month: "2-digit",
	year: "numeric",
	hour: "2-digit",
	minute: "2-digit",
});

function formatExpiryDate(expiresAt: string): string {
	const date = new Date(expiresAt);
	return Number.isNaN(date.getTime())
		? expiresAt
		: expiryDateFormatter.format(date);
}

function isOfficialResult(
	result: SellResult | SellOfficialResult,
): result is SellOfficialResult {
	return "transactionId" in result;
}

export default function SellModalSuccess({ result, playerName }: Props) {
	if (isOfficialResult(result)) {
		return (
			<div role="status" className={styles.success}>
				<div className={styles.success__header}>
					<div className={styles.success__icon}>
						<CheckCircle2 aria-hidden="true" />
					</div>
					<div className={styles.success__titleBlock}>
						<h4>Venta inmediata completada</h4>
						<span>
							Transacción #{result.transactionId} registrada exitosamente
						</span>
					</div>
				</div>

				<div className={styles.success__details}>
					<div className={styles.success__row}>
						<span>Jugador</span>
						<strong>{playerName}</strong>
					</div>
					<div className={styles.success__row}>
						<span>Tokens vendidos</span>
						<strong>
							{result.quantity} {result.quantity === 1 ? "token" : "tokens"}
						</strong>
					</div>
					<div className={styles.success__row}>
						<span>Cotización oficial</span>
						<span>{formatCredits(result.quotePrice)}</span>
					</div>
					<div className={styles.success__row}>
						<span>Comisión oficial</span>
						<span className={styles.success__spread}>
							-{result.discountPercentage}%
						</span>
					</div>
					<div className={styles.success__row}>
						<span>Precio unitario neto</span>
						<span>{formatCredits(result.unitPrice)}</span>
					</div>
					<div className={styles.success__divider} />
					<div className={styles.success__row}>
						<span>Créditos acreditados</span>
						<strong className={styles.success__total}>
							{formatCredits(result.totalAmount)}
						</strong>
					</div>
				</div>
			</div>
		);
	}

	return (
		<div role="status" className={styles.success}>
			<div className={styles.success__header}>
				<div className={styles.success__icon}>
					<CheckCircle2 aria-hidden="true" />
				</div>
				<div className={styles.success__titleBlock}>
					<h4>Publicación confirmada</h4>
					<span>Orden #{result.listingId} registrada en el mercado</span>
				</div>
			</div>

			<div className={styles.success__details}>
				<div className={styles.success__row}>
					<span>Jugador</span>
					<strong>{playerName}</strong>
				</div>
				<div className={styles.success__row}>
					<span>Tokens publicados</span>
					<strong>
						{result.quantity} {result.quantity === 1 ? "token" : "tokens"}
					</strong>
				</div>
				<div className={styles.success__row}>
					<span>Precio unitario</span>
					<span>{formatCredits(result.unitPrice)}</span>
				</div>
				<div className={styles.success__divider} />
				<div className={styles.success__row}>
					<span>Importe total</span>
					<strong className={styles.success__total}>
						{formatCredits(result.totalAmount)}
					</strong>
				</div>
				<div className={styles.success__row}>
					<span>Vencimiento</span>
					<span>{formatExpiryDate(result.expiresAt)}</span>
				</div>
			</div>
		</div>
	);
}
