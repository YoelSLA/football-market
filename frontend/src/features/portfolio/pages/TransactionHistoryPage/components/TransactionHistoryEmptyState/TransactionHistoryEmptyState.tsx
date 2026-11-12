import { Button } from "@/shared/components";
import { History } from "@/shared/icons";
import styles from "./TransactionHistoryEmptyState.module.scss";

interface Props {
	type: "empty" | "no-results";
	onAction: () => void;
}

export default function TransactionHistoryEmptyState({
	type,
	onAction,
}: Props) {
	const noResults = type === "no-results";
	return (
		<div role="status" className={styles.transactionHistoryEmptyState}>
			<History
				aria-hidden="true"
				className={styles.transactionHistoryEmptyState__icon}
			/>
			<h2 className={styles.transactionHistoryEmptyState__title}>
				{noResults
					? "No hay operaciones que coincidan"
					: "Todavía no tenés transacciones"}
			</h2>
			<p className={styles.transactionHistoryEmptyState__description}>
				{noResults
					? "Probá cambiando o limpiando los filtros."
					: "Tus compras y ventas confirmadas aparecerán en este historial."}
			</p>
			<Button
				type="button"
				variant="secondary"
				onClick={onAction}
				className={styles.transactionHistoryEmptyState__action}
			>
				{noResults ? "Limpiar filtros" : "Ir al mercado"}
			</Button>
		</div>
	);
}
