import type { ReactNode } from "react";
import type { TransactionHistoryEntry as TransactionHistoryEntryModel } from "@/features/portfolio/types";
import TransactionHistoryEmptyState from "../TransactionHistoryEmptyState";
import TransactionHistoryEntry from "../TransactionHistoryEntry";
import TransactionHistoryState from "../TransactionHistoryState";
import styles from "./TransactionHistoryContent.module.scss";

interface Props {
	toolbar?: ReactNode;
	isLoading: boolean;
	errorMessage: string | null;
	transactions?: TransactionHistoryEntryModel[];
	totalElements: number;
	hasFilters: boolean;
	onRetry: () => void;
	onGoToMarket: () => void;
	onClearFilters: () => void;
}

export default function TransactionHistoryContent({
	toolbar,
	isLoading,
	errorMessage,
	transactions,
	totalElements,
	hasFilters,
	onRetry,
	onGoToMarket,
	onClearFilters,
}: Props) {
	if (errorMessage) {
		return (
			<div className={styles.transactionHistoryContent}>
				{toolbar && (
					<div className={styles.transactionHistoryContent__toolbarWrapper}>
						{toolbar}
					</div>
				)}
				<TransactionHistoryState
					type="error"
					message={errorMessage}
					onRetry={onRetry}
				/>
			</div>
		);
	}

	if (isLoading || (transactions?.length === 0 && totalElements > 0)) {
		return (
			<div className={styles.transactionHistoryContent}>
				{toolbar && (
					<div className={styles.transactionHistoryContent__toolbarWrapper}>
						{toolbar}
					</div>
				)}
				<TransactionHistoryState type="loading" />
			</div>
		);
	}

	if (!transactions || totalElements === 0) {
		return (
			<div className={styles.transactionHistoryContent}>
				{toolbar && (
					<div className={styles.transactionHistoryContent__toolbarWrapper}>
						{toolbar}
					</div>
				)}
				<TransactionHistoryEmptyState
					type={hasFilters ? "no-results" : "empty"}
					onAction={hasFilters ? onClearFilters : onGoToMarket}
				/>
			</div>
		);
	}

	return (
		<section
			aria-label="Operaciones confirmadas"
			className={styles.transactionHistoryContent}
		>
			{toolbar && (
				<div className={styles.transactionHistoryContent__toolbarWrapper}>
					{toolbar}
				</div>
			)}
			<div className={styles.transactionHistoryContent__tableContainer}>
				<table
					className={styles.transactionHistoryContent__table}
					aria-label="Historial de transacciones"
				>
					<thead>
						<tr>
							<th scope="col" className={styles.th_id}>
								# ID
							</th>
							<th scope="col" className={styles.th_player}>
								Jugador
							</th>
							<th scope="col" className={styles.th_operation}>
								Operación
							</th>
							<th scope="col" className={styles.th_quantity}>
								Cantidad
							</th>
							<th scope="col" className={styles.th_unitPrice}>
								Precio unitario
							</th>
							<th scope="col" className={styles.th_total}>
								Total
							</th>
							<th scope="col" className={styles.th_date}>
								Fecha
							</th>
						</tr>
					</thead>
					<tbody>
						{transactions.map((transaction) => (
							<TransactionHistoryEntry
								key={transaction.transactionId}
								transaction={transaction}
							/>
						))}
					</tbody>
				</table>
			</div>
		</section>
	);
}
