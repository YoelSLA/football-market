import { PageLayout } from "@/app/layouts";
import { useMarketBuyPage } from "@/features/market/hooks";
import BuyTokenModal from "./components/BuyTokenModal";
import MarketHeader from "./components/MarketHeader";
import OfficialMarketPanel from "./components/OfficialMarketPanel";
import styles from "./MarketBuyPage.module.scss";

/**
 * Página de compra de tokens en el mercado oficial (emisión oficial del superusuario).
 * Coordina la cabecera, catálogo de emisión oficial y el diálogo modal de compra.
 */
export default function MarketBuyPage() {
	const marketBuy = useMarketBuyPage();

	return (
		<PageLayout>
			<div className={styles.page}>
				<div className={styles.page__header}>
					<MarketHeader
						totalPlayers={marketBuy.totalPlayers}
						availableCredits={marketBuy.availableCredits}
					/>
				</div>

				<OfficialMarketPanel marketBuy={marketBuy} />

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
		</PageLayout>
	);
}
