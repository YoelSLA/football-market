import { History } from "@/shared/icons";
import styles from "./TransactionHistoryHeader.module.scss";

interface Props {
	transactionCount?: number;
}

export default function TransactionHistoryHeader({ transactionCount }: Props) {
	const countLabel =
		transactionCount !== undefined
			? `${transactionCount} ${transactionCount === 1 ? "operación" : "operaciones"}`
			: null;

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
			{countLabel && (
				<p className={styles.transactionHistoryHeader__count}>{countLabel}</p>
			)}
		</header>
	);
}
