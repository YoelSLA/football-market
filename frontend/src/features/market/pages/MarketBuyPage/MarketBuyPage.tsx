import { useMarketBuyPage } from "@/features/market/hooks";
import type { MarketPlayer } from "@/features/market/types";
import { Button } from "@/shared/components";
import BuyTokenModal from "./BuyTokenModal";
import styles from "./MarketBuyPage.module.scss";
import MarketFilters from "./MarketFilters";
import MarketHeader from "./MarketHeader";
import MarketPagination from "./MarketPagination";
import MarketPlayerCard from "./MarketPlayerCard";

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
 * a MarketPlayerCard y BuyTokenModal exclusivamente por props.
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
				<main className={styles.page__main}>
					{isLoadingPlayers && (
						<div role="status" className={styles.page__status}>
							<p>Cargando catálogo del mercado...</p>
						</div>
					)}

					{!isLoadingPlayers && isPlayersError && (
						<div role="alert" className={styles.page__errorState}>
							<p>
								{errorMessage || "No se pudo cargar el listado del mercado."}
							</p>
							{onRetryPlayers && (
								<Button
									type="button"
									onClick={onRetryPlayers}
									className={styles.page__retryBtn}
								>
									Reintentar
								</Button>
							)}
						</div>
					)}

					{!isLoadingPlayers && !isPlayersError && players.length === 0 && (
						<div className={styles.page__status}>
							<p>No hay jugadores disponibles en el mercado actualmente.</p>
						</div>
					)}

					{!isLoadingPlayers && !isPlayersError && players.length > 0 && (
						<div className={styles.page__grid}>
							{players.map((player) => {
								const quoteState = marketBuy.quotesQuery.getQuoteState(
									player.id,
								);
								return (
									<MarketPlayerCard
										key={player.id}
										player={player}
										quote={quoteState.quote}
										quoteStatus={quoteState.status}
										quoteHttpStatus={quoteState.httpStatus}
										canRetryQuote={quoteState.canRetry}
										onRetryQuote={quoteState.onRetry}
										onBuyClick={marketBuy.openBuyModal}
									/>
								);
							})}
						</div>
					)}
				</main>
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
