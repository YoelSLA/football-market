import MarketFilters from "@/features/market/components/MarketFilters";
import MarketPagination from "@/features/market/components/MarketPagination";
import { useMarketBuyPage } from "@/features/market/hooks";
import type { MarketPlayer } from "@/features/market/types";
import BuyTokenModal from "./components/BuyTokenModal";
import MarketBuyCatalog from "./components/MarketBuyCatalog";
import MarketHeader from "./components/MarketHeader";
import styles from "./MarketBuyPage.module.scss";

interface Props {
	players: MarketPlayer[];
	isLoadingPlayers: boolean;
	isPlayersError: boolean;
	errorMessage?: string | null;
	onRetryPlayers?: () => void;
	currentPage: number;
	totalPlayers?: number;
	pageSize?: number;
	totalPages: number;
	onPageChange: (page: number) => void;
	onPurchaseSuccess?: (playerId: number) => void;
}

/**
 * Página principal de compra de tokens en el mercado (SPEC-019).
 * Recibe el catálogo paginado y callbacks desde app (compositor).
 * Conecta el hook de coordinación useMarketBuyPage y distribuye datos
 * a MarketHeader, MarketBuyCatalog y BuyTokenModal exclusivamente por props.
 */
export default function MarketBuyPage({
	players,
	isLoadingPlayers,
	isPlayersError,
	errorMessage,
	onRetryPlayers,
	currentPage,
	totalPlayers,
	pageSize,
	totalPages,
	onPageChange,
	onPurchaseSuccess,
}: Props) {
	const marketBuy = useMarketBuyPage({
		players,
		onPurchaseSuccess,
	});

	return (
		<div className={styles.page}>
			<div className={styles.page__header}>
				<MarketHeader totalPlayers={totalPlayers} />
			</div>
			<div className={styles.page__content}>
				<MarketFilters />
				<MarketBuyCatalog
					isLoading={isLoadingPlayers}
					isError={isPlayersError}
					errorMessage={errorMessage}
					onRetry={onRetryPlayers}
					players={players}
					getQuoteState={marketBuy.quotesQuery.getQuoteState}
					onBuyClick={marketBuy.openBuyModal}
				/>
			</div>
			<div className={styles.page__footer}>
				<MarketPagination
					page={currentPage}
					totalPages={totalPages}
					totalPlayers={totalPlayers}
					visiblePlayers={players.length}
					pageSize={pageSize}
					onPageChange={onPageChange}
				/>
			</div>

			<BuyTokenModal
				isOpen={marketBuy.isModalOpen}
				player={marketBuy.selectedPlayer}
				quote={marketBuy.selectedQuote}
				quoteStatus={marketBuy.selectedQuoteState?.status ?? "loading"}
				quoteHttpStatus={marketBuy.selectedQuoteState?.httpStatus}
				canRetryQuote={marketBuy.selectedQuoteState?.canRetry ?? false}
				onRetryQuote={marketBuy.selectedQuoteState?.onRetry ?? (() => {})}
				confirmationBlockReason={marketBuy.confirmationBlockReason}
				estimation={marketBuy.estimation}
				formRegister={marketBuy.form.register}
				formError={marketBuy.form.formState.errors.quantity?.message}
				isBusy={marketBuy.isBusy}
				canConfirm={marketBuy.canConfirm}
				isSuccess={marketBuy.isSuccess}
				purchaseResult={marketBuy.purchaseResult}
				errorMessage={marketBuy.errorMessage}
				onClose={marketBuy.closeBuyModal}
				onConfirm={marketBuy.handleConfirmBuy}
				triggerRef={marketBuy.triggerRef}
				initialFocusRef={marketBuy.initialFocusRef}
			/>
		</div>
	);
}
