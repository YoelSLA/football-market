import { useNavigate, useParams } from "react-router-dom";
import { PageLayout } from "@/app/layouts";
import { useHotPlayerIds } from "@/features/market/hooks/queries";
import BuyTokenModal from "@/features/market/pages/MarketBuyPage/components/BuyTokenModal";
import { usePlayerDetailPage } from "@/features/players/hooks";
import PlayerDetailNav from "./components/PlayerDetailNav";
import PlayerDetailStatus from "./components/PlayerDetailStatus";
import PlayerOverview from "./components/PlayerOverview";
import PlayerStatisticsSection from "./components/PlayerStatisticsSection";
import { usePlayerBuyModal, usePlayerHolding } from "./hooks";
import styles from "./PlayerDetailPage.module.scss";

export default function PlayerDetailPage() {
	const { id } = useParams();
	const navigate = useNavigate();
	const detail = usePlayerDetailPage(id);

	const { hotPlayerIds } = useHotPlayerIds();
	const isHot = detail.player ? hotPlayerIds.has(detail.player.id) : false;

	const { holding, availableCredits } = usePlayerHolding(detail.player?.id);
	const buyModal = usePlayerBuyModal(detail.player, detail.currentQuote);

	const handleSellClick = () => {
		if (detail.player) {
			navigate(`/market/p2p?search=${encodeURIComponent(detail.player.name)}`);
		}
	};

	return (
		<PageLayout>
			<div className={styles.playerDetail}>
				<div className={styles.playerDetail__inner}>
					<PlayerDetailNav
						playerName={detail.player?.name}
						leagueName={detail.player?.leagueName}
					/>

					{!detail.player ? (
						<div className={styles.playerDetail__status}>
							<PlayerDetailStatus
								state={detail.detailState}
								errorMessage={detail.errorMessage}
								onRetry={detail.retry}
							/>
						</div>
					) : (
						<div className={styles.playerDetail__container}>
							<PlayerOverview
								player={detail.player}
								statistics={detail.statistics}
								quote={detail.currentQuote}
								quoteLoading={detail.quotesState === "loading"}
								isHot={isHot}
								holding={holding}
								availableCredits={availableCredits}
								onBuyClick={() => buyModal.openBuyModal()}
								onSellClick={handleSellClick}
							/>

							<PlayerStatisticsSection
								player={detail.player}
								statistics={detail.statistics}
								statisticsState={detail.statisticsState}
								statisticsErrorMessage={detail.statisticsErrorMessage}
								quotes={detail.quotes}
								quotesState={detail.quotesState}
								quotesErrorMessage={detail.quotesErrorMessage}
								onRetryQuotes={detail.retryQuotes}
								activeTab={detail.activeTab}
								onTabChange={detail.setActiveTab}
								onRetryStatistics={detail.retryStatistics}
								search={detail.search}
								category={detail.category}
								onSearchChange={detail.setSearch}
								onCategoryChange={detail.setCategory}
								onClearFilters={detail.clearFilters}
							/>

							<BuyTokenModal
								isOpen={buyModal.isOpen}
								player={buyModal.officialPlayer}
								quote={buyModal.officialQuote}
								confirmationBlockReason={buyModal.confirmationBlockReason}
								estimation={buyModal.estimation}
								formRegister={buyModal.formRegister}
								formError={buyModal.formError}
								quantity={buyModal.quantity}
								onQuantityChange={buyModal.onQuantityChange}
								isBusy={buyModal.isBusy}
								canConfirm={buyModal.canConfirm}
								isSuccess={buyModal.isSuccess}
								purchaseResult={buyModal.purchaseResult}
								errorMessage={buyModal.errorMessage}
								onClose={buyModal.closeBuyModal}
								onConfirm={buyModal.handleConfirmBuy}
								triggerRef={buyModal.triggerRef}
								initialFocusRef={buyModal.initialFocusRef}
							/>
						</div>
					)}
				</div>
			</div>
		</PageLayout>
	);
}
