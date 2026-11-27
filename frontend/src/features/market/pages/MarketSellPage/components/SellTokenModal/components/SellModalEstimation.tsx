import type { SellEstimation } from "@/features/market/types";
import { formatCentimes, formatCredits } from "@/features/market/utils";
import { TokenSymbol } from "@/shared/components";
import { Receipt } from "@/shared/icons";
import styles from "./SellModalEstimation.module.scss";

interface Props {
	estimation: SellEstimation;
}

export default function SellModalEstimation({ estimation }: Props) {
	return (
		<div className={styles.estimation}>
			<div className={styles.estimation__header}>
				<Receipt aria-hidden="true" />
				<span>Resumen de la orden</span>
			</div>

			<div className={styles.estimation__body}>
				<div className={styles.estimation__row}>
					<span>
						<TokenSymbol size="xs" /> a publicar
					</span>
					<strong>
						{estimation.quantity} <TokenSymbol />
					</strong>
				</div>

				<div className={styles.estimation__row}>
					<span>Precio unitario congelado</span>
					<span>{formatCredits(estimation.unitPriceCentimes)}</span>
				</div>

				<div className={styles.estimation__divider} />

				<div className={styles.estimation__totalRow}>
					<div className={styles.estimation__totalHeader}>
						<span className={styles.estimation__totalLabel}>
							Total a recibir
						</span>
						<p className={styles.estimation__disclaimer}>
							Precio fijado por el servidor al publicar · Válido por 24 hs
						</p>
					</div>
					<div className={styles.estimation__totalAmount}>
						<strong>{formatCredits(estimation.totalCentimes)}</strong>
						<small>({formatCentimes(estimation.totalCentimes)})</small>
					</div>
				</div>
			</div>
		</div>
	);
}
