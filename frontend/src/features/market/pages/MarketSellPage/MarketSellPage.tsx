import { PageLayout } from "@/app/layouts";
import MarketFilters from "@/features/market/components/MarketFilters";
import MarketPagination from "@/features/market/components/MarketPagination";
import { useMarketSellPage } from "@/features/market/hooks";
import MarketSellCatalog from "./components/MarketSellCatalog";
import MarketSellHeader from "./components/MarketSellHeader";
import SellTokenModal from "./components/SellTokenModal";
import styles from "./MarketSellPage.module.scss";

export type { PlayerHolding } from "@/features/market/types";

export default function MarketSellPage() {
	const sell = useMarketSellPage();

	return (
		<PageLayout>
			<div className={styles.sellPage}>
				<div className={styles.sellPage__header}>
					<MarketSellHeader players={sell.allHoldings} />
				</div>

				<MarketFilters
					variant="sell"
					active
					options={sell.filterOptions}
					leagues={sell.leagues}
					positions={sell.positions}
					search={sell.searchInput}
					league={sell.filters.league}
					team={sell.filters.team}
					position={sell.filters.position}
					availability={sell.filters.availability}
					sort={sell.filters.sort}
					onSearchChange={sell.setSearchInput}
					onLeagueChange={(value) => sell.updateFilter("league", value)}
					onTeamChange={(value) => sell.updateFilter("team", value)}
					onPositionChange={(value) => sell.updateFilter("position", value)}
					onAvailabilityChange={(value) =>
						sell.updateFilter("availability", value)
					}
					onSortChange={(value) => sell.updateFilter("sort", value)}
					onApply={sell.applyFilters}
					onClear={sell.clearFilters}
					summary={`${sell.totalPlayers} ${
						sell.totalPlayers === 1
							? "jugador disponible"
							: "jugadores disponibles"
					}`}
				/>

				<div className={styles.sellPage__content}>
					<MarketSellCatalog
						isLoading={sell.isLoadingPlayers}
						isError={sell.isPlayersError}
						errorMessage={sell.playersErrorMessage}
						onRetry={sell.retryPlayers}
						players={sell.players}
						getQuoteState={sell.quoteQuery.getQuoteState}
						onSell={sell.open}
					/>
				</div>

				{!sell.isLoadingPlayers &&
					!sell.isPlayersError &&
					sell.totalPlayers > 0 && (
						<div className={styles.sellPage__footer}>
							<MarketPagination
								page={sell.page}
								totalPages={sell.totalPages}
								visiblePlayers={sell.players.length}
								pageSize={sell.pageSize}
								showSummary={false}
								variant="official"
								onPageChange={sell.setPage}
							/>
						</div>
					)}

				<SellTokenModal
					isOpen={sell.isOpen}
					player={sell.selectedPlayer}
					availableQuantity={sell.selectedPlayer?.availableQuantity ?? 0}
					reservedQuantity={sell.selectedPlayer?.reservedQuantity ?? 0}
					quote={sell.selectedQuote}
					quoteStatus={sell.selectedQuoteState?.status ?? "loading"}
					quoteHttpStatus={sell.selectedQuoteState?.httpStatus}
					canRetryQuote={sell.selectedQuoteState?.canRetry ?? false}
					onRetryQuote={sell.selectedQuoteState?.onRetry ?? (() => {})}
					estimation={sell.estimation}
					officialEstimation={sell.officialEstimation}
					sellMode={sell.sellMode}
					onSellModeChange={sell.setSellMode}
					p2pUnitPriceCentimes={sell.p2pUnitPriceCentimes}
					onP2PUnitPriceChange={sell.setP2PUnitPriceCentimes}
					formRegister={sell.form.register}
					formError={
						sell.form.formState.errors.quantity?.message ??
						(Number(sell.form.watch("quantity")) > sell.form.maximum
							? "Cantidad superior a los tokens disponibles."
							: undefined)
					}
					quantity={sell.form.watch("quantity")}
					maximum={sell.form.maximum}
					presets={sell.form.presets}
					onQuantityChange={sell.form.setQuantity}
					result={sell.result}
					errorMessage={sell.errorMessage}
					confirmationBlockReason={sell.confirmationBlockReason}
					isBusy={sell.isBusy}
					canConfirm={sell.canConfirm}
					onClose={sell.close}
					onConfirm={sell.confirm}
					triggerRef={sell.triggerRef}
					initialFocusRef={sell.initialFocusRef}
				/>
			</div>
		</PageLayout>
	);
}
