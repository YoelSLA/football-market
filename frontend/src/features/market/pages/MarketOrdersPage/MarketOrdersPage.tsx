import { PageLayout } from "@/app/layouts";
import MarketFilters from "@/features/market/components/MarketFilters";
import { useMyOrdersPage } from "@/features/market/hooks";
import CancelListingModal from "./components/CancelListingModal";
import MarketOrdersEmptyState from "./components/MarketOrdersEmptyState";
import MarketOrdersHeader from "./components/MarketOrdersHeader";
import MarketOrdersMetrics from "./components/MarketOrdersMetrics";
import MarketOrdersTable from "./components/MarketOrdersTable";
import styles from "./MarketOrdersPage.module.scss";

export default function MarketOrdersPage() {
	const {
		page,
		totalPages,
		totalElements,
		availableCredits,
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
	const hasNoOrders = !isLoading && !isError && orders.length === 0;

	return (
		<PageLayout>
			<div className={styles.ordersPage}>
				<div className={styles.ordersPage__header}>
					<MarketOrdersHeader availableCredits={availableCredits} />
				</div>

				<MarketOrdersMetrics summary={summary} isLoading={isLoading} />

				<MarketFilters
					variant="orders"
					search={searchInput}
					status={status}
					sort={sort}
					onSearchChange={setSearchInput}
					onStatusChange={setStatus}
					onSortChange={setSort}
					onClear={clearFilters}
					summary={`${summary.totalOrders} ${
						summary.totalOrders === 1
							? "orden registrada"
							: "órdenes registradas"
					}`}
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
