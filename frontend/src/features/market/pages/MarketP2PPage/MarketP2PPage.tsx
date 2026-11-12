import { PageLayout } from "@/app/layouts";
import { useCommunityMarketPage } from "@/features/market/hooks";
import {
	BuyListingModal,
	CommunityMarketPanel,
	MarketP2PHeader,
} from "./components";
import styles from "./MarketP2PPage.module.scss";

/**
 * Página del mercado P2P / entre usuarios.
 * Coordina la cabecera con métricas comunitarias, el panel de ofertas y publicaciones de usuarios
 * y el diálogo modal de compra de listings.
 */
export default function MarketP2PPage() {
	const community = useCommunityMarketPage();

	return (
		<PageLayout>
			<div className={styles.page}>
				<div className={styles.page__header}>
					<MarketP2PHeader
						totalPlayers={community.totalPlayers}
						availableCredits={community.availableCredits}
					/>
				</div>

				<CommunityMarketPanel community={community} />

				<BuyListingModal
					player={community.selectedPlayer}
					listing={community.selectedListing}
					isBusy={community.isBuying}
					result={community.purchaseResult}
					errorMessage={community.purchaseError}
					availableCredits={community.availableCredits}
					onClose={community.closeListing}
					onConfirm={community.confirmListing}
					triggerRef={community.triggerRef}
				/>
			</div>
		</PageLayout>
	);
}
