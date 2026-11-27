import { PageLayout } from "@/app/layouts";
import { useMarketBuyPage } from "@/features/market/hooks";
import { usePortfolioSummaryQuery } from "@/features/portfolio/hooks/queries";
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
	const portfolioQuery = usePortfolioSummaryQuery();

	return (
		<PageLayout>
			<div className={styles.page}>
				<div className={styles.page__header}>
					<MarketHeader
						availableCredits={marketBuy.availableCredits}
						totalPlayers={marketBuy.totalPlayers}
						priceRange={marketBuy.priceRange}
						portfolioSummary={portfolioQuery.data?.summary}
					/>
				</div>

				<OfficialMarketPanel marketBuy={marketBuy} />

				<BuyTokenModal
					isOpen={marketBuy.isModalOpen}
					player={marketBuy.selectedPlayer}
					quote={marketBuy.selectedQuote}
					confirmationBlockReason={marketBuy.confirmationBlockReason}
					estimation={marketBuy.estimation}
					formRegister={marketBuy.form.register}
					formError={marketBuy.form.formState.errors.quantity?.message}
					quantity={marketBuy.form.watch("quantity")}
					onQuantityChange={marketBuy.form.setQuantity}
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
