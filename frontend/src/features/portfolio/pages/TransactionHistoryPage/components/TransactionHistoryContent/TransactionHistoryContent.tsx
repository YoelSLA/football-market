import type { TransactionHistoryEntry as TransactionHistoryEntryModel } from "@/features/portfolio/types";
import { Pagination } from "@/shared/components";
import TransactionHistoryEmptyState from "../TransactionHistoryEmptyState";
import TransactionHistoryEntry from "../TransactionHistoryEntry";
import TransactionHistoryState from "../TransactionHistoryState";
import styles from "./TransactionHistoryContent.module.scss";

interface Props {
	isLoading: boolean;
	errorMessage: string | null;
	transactions?: TransactionHistoryEntryModel[];
	page: number;
	pageSize: number;
	totalElements: number;
	totalPages: number;
	hasFilters: boolean;
	onRetry: () => void;
	onGoToMarket: () => void;
	onClearFilters: () => void;
	onPageChange: (page: number) => void;
}

export default function TransactionHistoryContent({
	isLoading,
	errorMessage,
	transactions,
	page,
	pageSize,
	totalElements,
	totalPages,
	hasFilters,
	onRetry,
	onGoToMarket,
	onClearFilters,
	onPageChange,
}: Props) {
	if (errorMessage) {
		return (
			<div className={styles.transactionHistoryContent}>
				<TransactionHistoryState
					type="error"
					message={errorMessage}
					onRetry={onRetry}
				/>
			</div>
		);
	}

	if (isLoading) {
		return (
			<div className={styles.transactionHistoryContent}>
				<TransactionHistoryState type="loading" />
			</div>
		);
	}

	if (transactions?.length === 0 && totalElements > 0) {
		return (
			<div className={styles.transactionHistoryContent}>
				<TransactionHistoryState type="loading" />
			</div>
		);
	}

	if (!transactions || totalElements === 0) {
		return (
			<div className={styles.transactionHistoryContent}>
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
			<div
				aria-hidden="true"
				className={styles.transactionHistoryContent__headerRow}
			>
				<span>Jugador</span>
				<span>Operación</span>
				<span>Cantidad</span>
				<span>Precio unitario</span>
				<span>Total</span>
				<span>Fecha</span>
			</div>
			<ol className={styles.transactionHistoryContent__list}>
				{transactions.map((transaction) => (
					<TransactionHistoryEntry
						key={transaction.transactionId}
						transaction={transaction}
					/>
				))}
			</ol>
			<footer className={styles.transactionHistoryContent__footer}>
				<p className={styles.transactionHistoryContent__range}>
					Mostrando {page * pageSize + 1}–
					{Math.min((page + 1) * pageSize, totalElements)} de {totalElements}
				</p>
				<Pagination
					page={page}
					totalPages={totalPages}
					onChange={onPageChange}
				/>
			</footer>
		</section>
	);
}
