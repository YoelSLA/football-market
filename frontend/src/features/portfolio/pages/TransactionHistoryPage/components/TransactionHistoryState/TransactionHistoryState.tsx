import { Button } from "@/shared/components";
import styles from "./TransactionHistoryState.module.scss";

interface Props {
	type: "loading" | "error";
	message?: string;
	onRetry?: () => void;
}

export default function TransactionHistoryState({
	type,
	message,
	onRetry,
}: Props) {
	if (type === "loading") {
		return (
			<div aria-busy="true" className={styles.transactionHistoryState}>
				<p role="status" className={styles.transactionHistoryState__loading}>
					{message ?? "Consultando tu historial de transacciones…"}
				</p>
			</div>
		);
	}

	return (
		<div role="alert" className={styles.transactionHistoryState}>
			<p className={styles.transactionHistoryState__errorMessage}>
				{message ?? "No pudimos cargar tu historial."}
			</p>
			<p className={styles.transactionHistoryState__errorDetail}>
				Tus operaciones no fueron modificadas.
			</p>
			{onRetry && (
				<Button
					type="button"
					onClick={onRetry}
					className={styles.transactionHistoryState__retryButton}
				>
					Reintentar
				</Button>
			)}
		</div>
	);
}
