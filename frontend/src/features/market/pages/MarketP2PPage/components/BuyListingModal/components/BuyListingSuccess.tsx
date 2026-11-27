import type { BuyResult } from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { Button, TokenSymbol } from "@/shared/components";
import styles from "./BuyListingSuccess.module.scss";

interface Props {
	result: BuyResult;
	returnsToCatalog: boolean;
	onClose: () => void;
}

export function BuyListingSuccess({
	result,
	returnsToCatalog,
	onClose,
}: Props) {
	return (
		<div className={styles.success} role="status">
			<h3>Compra confirmada</h3>
			<p>Transacción #{result.transactionId}</p>
			<dl>
				<div>
					<dt>Cantidad</dt>
					<dd>
						{result.quantity} <TokenSymbol />
					</dd>
				</div>
				<div>
					<dt>Precio unitario</dt>
					<dd>{formatCredits(result.unitPrice)}</dd>
				</div>
				<div>
					<dt>Total</dt>
					<dd>{formatCredits(result.totalAmount)}</dd>
				</div>
				<div>
					<dt>Ejecutada</dt>
					<dd>{new Date(result.executedAt).toLocaleString("es-AR")}</dd>
				</div>
			</dl>
			<Button onClick={onClose}>
				{returnsToCatalog ? "Volver al catálogo" : "Volver a ofertas"}
			</Button>
		</div>
	);
}
