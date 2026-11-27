import type { TransactionHistorySummary } from "@/features/portfolio/types";
import { History } from "@/shared/icons";
import TransactionHistoryMetrics from "../TransactionHistoryMetrics";
import styles from "./TransactionHistoryHeader.module.scss";

interface Props {
	summary?: TransactionHistorySummary;
	isLoading?: boolean;
}

export default function TransactionHistoryHeader({
	summary,
	isLoading,
}: Props) {
	return (
		<header className={styles.transactionHistoryHeader}>
			<div className={styles.transactionHistoryHeader__left}>
				<div className={styles.transactionHistoryHeader__iconWrapper}>
					<History
						aria-hidden="true"
						className={styles.transactionHistoryHeader__icon}
					/>
				</div>
				<div className={styles.transactionHistoryHeader__titleWrapper}>
					<h1
						id="transaction-history-title"
						className={styles.transactionHistoryHeader__title}
					>
						Historial de transacciones
					</h1>
					<p className={styles.transactionHistoryHeader__description}>
						Compras y ventas confirmadas de tu cuenta.
					</p>
				</div>
			</div>
			{summary && (
				<div className={styles.transactionHistoryHeader__metrics}>
					<TransactionHistoryMetrics summary={summary} isLoading={isLoading} />
				</div>
			)}
		</header>
	);
}
