import { PageLayout } from "@/app/layouts";
import {
	useCommunityMarketPage,
	useMarketBuyPage,
} from "@/features/market/hooks";
import BuyTokenModal from "./components/BuyTokenModal";
import {
	BuyListingModal,
	CommunityMarketPanel,
} from "./components/CommunityMarket";
import MarketHeader from "./components/MarketHeader";
import MarketSelector from "./components/MarketSelector";
import OfficialMarketPanel from "./components/OfficialMarketPanel";
import styles from "./MarketBuyPage.module.scss";

/**
 * Página principal de compra de tokens en el mercado.
 * Coordina la cabecera, selector de modo y diálogos modales,
 * delegando la vista de emisión oficial a OfficialMarketPanel
 * y la oferta de usuarios a CommunityMarketPanel.
 */
export default function MarketBuyPage() {
	const marketBuy = useMarketBuyPage();
	const community = useCommunityMarketPage(marketBuy.marketTab === "community");

	return (
		<PageLayout>
			<div className={styles.page}>
				<div className={styles.page__header}>
					<MarketHeader
						totalPlayers={marketBuy.totalPlayers}
						availableCredits={marketBuy.availableCredits}
					/>
				</div>
				<div className={styles.page__selector}>
					<MarketSelector
						selectedTab={marketBuy.marketTab}
						onSelectTab={marketBuy.setMarketTab}
					/>
				</div>

				{marketBuy.marketTab === "official" ? (
					<OfficialMarketPanel marketBuy={marketBuy} />
				) : (
					<CommunityMarketPanel community={community} />
				)}

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
				<BuyListingModal
					player={community.selectedPlayer}
					listing={community.selectedListing}
					isBusy={community.isBuying}
					result={community.purchaseResult}
					errorMessage={community.purchaseError}
					availableCredits={marketBuy.availableCredits}
					onClose={community.closeListing}
					onConfirm={community.confirmListing}
					triggerRef={community.triggerRef}
				/>
			</div>
		</PageLayout>
	);
}
