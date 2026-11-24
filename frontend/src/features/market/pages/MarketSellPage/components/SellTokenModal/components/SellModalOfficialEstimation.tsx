import type { SellOfficialEstimation } from "@/features/market/types";
import { formatCentimes, formatCredits } from "@/features/market/utils";
import { TOKEN_SYMBOL } from "@/shared/constants";
import { Store } from "@/shared/icons";
import styles from "./SellModalOfficialEstimation.module.scss";

interface Props {
	estimation: SellOfficialEstimation;
}

export default function SellModalOfficialEstimation({ estimation }: Props) {
	return (
		<div
			className={styles.estimation}
			id="panel-official"
			role="tabpanel"
			aria-labelledby="tab-official"
		>
			<div className={styles.estimation__header}>
				<Store aria-hidden="true" />
				<span>Desglose de venta oficial inmediata</span>
			</div>

			<div className={styles.estimation__body}>
				<div className={styles.estimation__row}>
					<span>Tokens a vender</span>
					<strong>
						{estimation.quantity} {TOKEN_SYMBOL}
					</strong>
				</div>

				<div className={styles.estimation__row}>
					<span>Cotización oficial</span>
					<span>{formatCredits(estimation.officialPriceCentimes)}</span>
				</div>

				<div className={styles.estimation__row}>
					<span>Comisión oficial ({estimation.discountPercentage}%)</span>
					<span className={styles.estimation__spread}>
						- {formatCredits(estimation.spreadCentimes)}
					</span>
				</div>

				<div className={styles.estimation__row}>
					<span>Valor unitario neto</span>
					<strong>{formatCredits(estimation.unitPriceNetCentimes)}</strong>
				</div>

				<div className={styles.estimation__divider} />

				<div className={styles.estimation__totalRow}>
					<div className={styles.estimation__totalHeader}>
						<span className={styles.estimation__totalLabel}>
							Créditos netos a percibir
						</span>
						<p className={styles.estimation__disclaimer}>
							Venta instantánea a la casa oficial · Acreditación inmediata en tu
							saldo
						</p>
					</div>
					<div className={styles.estimation__totalAmount}>
						<strong>{formatCredits(estimation.totalNetCentimes)}</strong>
						<small>({formatCentimes(estimation.totalNetCentimes)})</small>
					</div>
				</div>
			</div>
		</div>
	);
}
