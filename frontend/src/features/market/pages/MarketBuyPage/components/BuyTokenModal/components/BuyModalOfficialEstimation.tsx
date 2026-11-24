import type { OfficialMarketQuote } from "@/features/market/types";
import { formatCentimes, formatCredits } from "@/features/market/utils";
import { CREDIT_SYMBOL, TOKEN_SYMBOL } from "@/shared/constants";
import { Store } from "@/shared/icons";
import styles from "./BuyModalOfficialEstimation.module.scss";

interface Props {
	quote: OfficialMarketQuote;
	quantity: number;
	totalCentimes: number;
}

export function BuyModalOfficialEstimation({
	quote,
	quantity,
	totalCentimes,
}: Props) {
	return (
		<div className={styles.estimationSlot}>
			<div className={styles.estimation}>
				<div className={styles.estimationHeader}>
					<Store aria-hidden="true" />
					<span>Resumen de orden de compra</span>
				</div>

				<div className={styles.estimationBody}>
					<div className={styles.detailRow}>
						<span>Tokens a comprar</span>
						<strong>
							{quantity} {TOKEN_SYMBOL}
						</strong>
					</div>
					<div className={styles.detailRow}>
						<span>Cotización oficial unitaria</span>
						<span>{formatCredits(quote.price)}</span>
					</div>
					<div className={styles.detailRow}>
						<span>Comisión de emisión</span>
						<span className={styles.zeroFee}>
							0,00 {CREDIT_SYMBOL} (Sin costo adicional)
						</span>
					</div>

					<div className={styles.detailDivider} />

					<div className={styles.totalRow}>
						<div className={styles.totalHeader}>
							<span className={styles.totalLabel}>Total a pagar</span>
							<p className={styles.totalDisclaimer}>
								El servidor confirma precio y disponibilidad al comprar
							</p>
						</div>
						<div className={styles.totalAmount}>
							<strong>{formatCredits(totalCentimes)}</strong>
							<small>({formatCentimes(totalCentimes)})</small>
						</div>
					</div>
				</div>
			</div>
		</div>
	);
}
