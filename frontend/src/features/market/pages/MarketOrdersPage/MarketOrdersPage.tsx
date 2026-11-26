import { PageLayout } from "@/app/layouts";
import MarketPagination from "@/features/market/components/MarketPagination";
import { useMyOrdersPage } from "@/features/market/hooks";
import CancelListingModal from "./components/CancelListingModal";
import MarketOrdersEmptyState from "./components/MarketOrdersEmptyState";
import MarketOrdersFilters from "./components/MarketOrdersFilters";
import MarketOrdersHeader from "./components/MarketOrdersHeader";
import MarketOrdersTable from "./components/MarketOrdersTable";
import styles from "./MarketOrdersPage.module.scss";

export default function MarketOrdersPage() {
	const {
		page,
		totalPages,
		totalElements,
		summary,
		orders,
		status,
		sort,
		searchInput,
		isLoading,
		isError,
		refetch,
		setPage,
		setStatus,
		setSort,
		setSearchInput,
		clearFilters,
		selectedOrderToCancel,
		isCancelModalOpen,
		isCancelling,
		openCancelModal,
		closeCancelModal,
		confirmCancel,
	} = useMyOrdersPage();

	const isFiltering = Boolean(
		status || searchInput.trim() || (sort && sort !== "NEWEST"),
	);

	return (
		<PageLayout>
			<div className={styles.ordersPage}>
				<div className={styles.ordersPage__header}>
					<MarketOrdersHeader summary={summary} isLoading={isLoading} />
				</div>

				<MarketOrdersTable
					toolbar={
						<MarketOrdersFilters
							search={searchInput}
							status={status}
							sort={sort}
							hasFilters={isFiltering}
							onSearchChange={setSearchInput}
							onStatusChange={setStatus}
							onSortChange={setSort}
							onClear={clearFilters}
						/>
					}
					isLoading={isLoading}
					errorMessage={
						isError ? "Ocurrió un error al cargar tus órdenes de venta." : null
					}
					onRetry={() => refetch()}
					orders={orders}
					emptyState={
						<MarketOrdersEmptyState
							isFiltering={isFiltering}
							onClearFilters={clearFilters}
						/>
					}
					onCancel={openCancelModal}
				/>

				{!isLoading && !isError && totalElements > 0 && (
					<div className={styles.ordersPage__footer}>
						<MarketPagination
							page={page}
							totalPages={totalPages}
							visiblePlayers={orders.length}
							totalPlayers={totalElements}
							pageSize={10}
							showSummary={false}
							variant="official"
							onPageChange={setPage}
						/>
					</div>
				)}

				<CancelListingModal
					isOpen={isCancelModalOpen}
					order={selectedOrderToCancel}
					isBusy={isCancelling}
					onClose={closeCancelModal}
					onConfirm={confirmCancel}
				/>
			</div>
		</PageLayout>
	);
}
