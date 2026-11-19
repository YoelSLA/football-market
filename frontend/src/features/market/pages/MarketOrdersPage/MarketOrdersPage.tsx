import { PageLayout } from "@/app/layouts";
import { useMyOrdersPage } from "@/features/market/hooks";
import CancelListingModal from "./components/CancelListingModal";
import MarketOrdersEmptyState from "./components/MarketOrdersEmptyState";
import MarketOrdersFilters from "./components/MarketOrdersFilters";
import MarketOrdersHeader from "./components/MarketOrdersHeader";
import MarketOrdersMetrics from "./components/MarketOrdersMetrics";
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
		searchInput,
		isLoading,
		isError,
		refetch,
		setPage,
		setStatus,
		setSearchInput,
		clearFilters,
		selectedOrderToCancel,
		isCancelModalOpen,
		isCancelling,
		openCancelModal,
		closeCancelModal,
		confirmCancel,
	} = useMyOrdersPage();

	const isFiltering = Boolean(status || searchInput.trim());
	const hasNoOrders = !isLoading && !isError && orders.length === 0;

	return (
		<PageLayout>
			<div className={styles.ordersPage}>
				<MarketOrdersHeader totalOrders={summary.totalOrders} />

				<MarketOrdersMetrics summary={summary} isLoading={isLoading} />

				<MarketOrdersFilters
					search={searchInput}
					status={status}
					onSearchChange={setSearchInput}
					onStatusChange={setStatus}
					onClear={clearFilters}
				/>

				{isError ? (
					<div className={styles.ordersPage__error}>
						<p>Ocurrió un error al cargar tus órdenes de venta.</p>
						<button
							type="button"
							onClick={() => refetch()}
							className={styles.ordersPage__retryBtn}
						>
							Reintentar consulta
						</button>
					</div>
				) : hasNoOrders ? (
					<MarketOrdersEmptyState
						isFiltering={isFiltering}
						onClearFilters={clearFilters}
					/>
				) : (
					<MarketOrdersTable
						orders={orders}
						page={page}
						totalPages={totalPages}
						totalElements={totalElements}
						onPageChange={setPage}
						onCancel={openCancelModal}
					/>
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
