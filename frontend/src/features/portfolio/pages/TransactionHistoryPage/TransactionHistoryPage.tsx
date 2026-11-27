import { useNavigate } from "react-router-dom";
import { PageLayout } from "@/app/layouts";
import MarketPagination from "@/features/market/components/MarketPagination";
import { useTransactionHistoryPage } from "@/features/portfolio/hooks";
import TransactionHistoryContent from "./components/TransactionHistoryContent";
import TransactionHistoryFilters from "./components/TransactionHistoryFilters";
import TransactionHistoryHeader from "./components/TransactionHistoryHeader";
import styles from "./TransactionHistoryPage.module.scss";

export default function TransactionHistoryPage() {
	const navigate = useNavigate();
	const page = useTransactionHistoryPage();
	const data = page.errorMessage ? undefined : page.data;
	const totalPages = data?.pagination.totalPages ?? 0;
	const totalElements = data?.pagination.totalElements ?? 0;

	return (
		<PageLayout>
			<section
				className={styles.transactionHistoryPage}
				aria-labelledby="transaction-history-title"
				aria-busy={page.isLoading || page.isRefreshing}
			>
				<TransactionHistoryHeader
					summary={data?.summary}
					isLoading={page.isLoading}
				/>
				<TransactionHistoryContent
					toolbar={
						<TransactionHistoryFilters
							{...page.criteria}
							hasFilters={page.hasFilters}
							onChange={page.updateCriterion}
							onClear={page.clearFilters}
						/>
					}
					isLoading={page.isLoading}
					errorMessage={page.errorMessage}
					transactions={data?.transactions}
					totalElements={totalElements}
					hasFilters={page.hasFilters}
					onRetry={() => {
						void page.retry();
					}}
					onGoToMarket={() => navigate("/market/buy")}
					onClearFilters={page.clearFilters}
				/>

				{!page.isLoading && !page.errorMessage && totalElements > 0 && (
					<div className={styles.transactionHistoryPage__footer}>
						<MarketPagination
							page={data?.pagination.page ?? page.criteria.page}
							totalPages={totalPages}
							visiblePlayers={data?.transactions?.length ?? 0}
							totalPlayers={totalElements}
							pageSize={data?.pagination.size ?? 12}
							showSummary={false}
							variant="official"
							onPageChange={page.selectPage}
						/>
					</div>
				)}
			</section>
		</PageLayout>
	);
}
