import { useMemo } from "react";
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

	const totalP2PTokens = useMemo(() => {
		return community.players.reduce(
			(sum, p) => sum + (p.availableQuantity || 0),
			0,
		);
	}, [community.players]);

	return (
		<PageLayout>
			<div className={styles.page}>
				<div className={styles.page__header}>
					<MarketP2PHeader
						availableCredits={community.availableCredits}
						totalPlayers={community.totalPlayers}
						priceRange={community.priceRange}
						totalP2PTokens={totalP2PTokens}
					/>
				</div>

				<CommunityMarketPanel community={community} />

				<BuyListingModal
					player={community.purchasePlayer}
					listing={community.selectedListing}
					isBusy={community.isBuying}
					result={community.purchaseResult}
					returnsToCatalog={community.purchaseReturnsToCatalog}
					errorMessage={community.purchaseError}
					availableCreditsCentimes={community.availableCreditsCentimes}
					canConfirm={community.canConfirmListing}
					onClose={community.closeListing}
					onConfirm={community.confirmListing}
					triggerRef={community.triggerRef}
				/>
			</div>
		</PageLayout>
	);
}
