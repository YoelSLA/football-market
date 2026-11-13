import { useNavigate } from "react-router-dom";
import { PageLayout } from "@/app/layouts";
import { useTransactionHistoryPage } from "@/features/portfolio/hooks";
import TransactionHistoryContent from "./components/TransactionHistoryContent";
import TransactionHistoryFilters from "./components/TransactionHistoryFilters";
import TransactionHistoryHeader from "./components/TransactionHistoryHeader";
import TransactionHistoryMetrics from "./components/TransactionHistoryMetrics";
import styles from "./TransactionHistoryPage.module.scss";

export default function TransactionHistoryPage() {
	const navigate = useNavigate();
	const page = useTransactionHistoryPage();
	const data = page.errorMessage ? undefined : page.data;

	return (
		<PageLayout>
			<section
				className={styles.transactionHistoryPage}
				aria-labelledby="transaction-history-title"
				aria-busy={page.isLoading || page.isRefreshing}
			>
				<TransactionHistoryHeader
					transactionCount={data?.summary.totalTransactions}
				/>
				{data && <TransactionHistoryMetrics summary={data.summary} />}
				<TransactionHistoryFilters
					{...page.criteria}
					hasFilters={page.hasFilters}
					onChange={page.updateCriterion}
					onClear={page.clearFilters}
				/>
				<TransactionHistoryContent
					isLoading={page.isLoading}
					errorMessage={page.errorMessage}
					transactions={data?.transactions}
					page={data?.pagination.page ?? page.criteria.page}
					pageSize={data?.pagination.size ?? 20}
					totalElements={data?.pagination.totalElements ?? 0}
					totalPages={data?.pagination.totalPages ?? 0}
					hasFilters={page.hasFilters}
					onRetry={() => {
						void page.retry();
					}}
					onGoToMarket={() => navigate("/market/buy")}
					onClearFilters={page.clearFilters}
					onPageChange={page.selectPage}
				/>
			</section>
		</PageLayout>
	);
}
