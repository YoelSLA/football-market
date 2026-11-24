import type { RefObject } from "react";
import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import type {
	BuyResult,
	CommunityMarketPlayer,
	MarketListing,
} from "@/features/market/types";
import { Modal } from "@/shared/components";
import styles from "./BuyListingModal.module.scss";
import {
	BuyListingBreakdown,
	BuyListingFooter,
	BuyListingHeader,
	BuyListingSuccess,
} from "./components";

interface Props {
	player: CommunityMarketPlayer | null;
	listing: MarketListing | null;
	isBusy: boolean;
	result: BuyResult | null;
	returnsToCatalog: boolean;
	errorMessage: string | null;
	availableCreditsCentimes?: number;
	canConfirm: boolean;
	onClose: () => void;
	onConfirm: () => void;
	triggerRef: RefObject<HTMLElement | null>;
}

export default function BuyListingModal({
	player,
	listing,
	isBusy,
	result,
	returnsToCatalog,
	errorMessage,
	availableCreditsCentimes,
	canConfirm,
	onClose,
	onConfirm,
	triggerRef,
}: Props) {
	if (!player || !listing) return null;
	const isExpired = Date.parse(listing.expiresAt) <= Date.now();
	const hasInsufficientCredits =
		availableCreditsCentimes !== undefined &&
		availableCreditsCentimes < listing.totalAmount;
	const estimatedRemaining =
		availableCreditsCentimes === undefined
			? null
			: availableCreditsCentimes - listing.totalAmount;

	return (
		<Modal
			className={styles.dialog}
			isOpen
			onClose={onClose}
			title={`Confirmar compra P2P, publicación #${listing.listingId}`}
			isBusy={isBusy}
			closeOnEsc={!isBusy}
			closeOnOverlayClick={!isBusy}
			triggerRef={triggerRef}
		>
			<section
				className={styles.modal}
				aria-label="Confirmación de compra de publicación"
			>
				<BuyListingHeader
					listingId={listing.listingId}
					isBusy={isBusy}
					onClose={onClose}
				/>

				<div className={styles.modal__player}>
					<MarketPlayerSummary player={player} variant="communityCard" />
				</div>

				{result ? (
					<BuyListingSuccess
						result={result}
						returnsToCatalog={returnsToCatalog}
						onClose={onClose}
					/>
				) : (
					<>
						<BuyListingBreakdown
							listing={listing}
							availableCreditsCentimes={availableCreditsCentimes}
							estimatedRemaining={estimatedRemaining}
							errorMessage={errorMessage}
							isExpired={isExpired}
							hasInsufficientCredits={hasInsufficientCredits}
							canConfirm={canConfirm}
						/>

						<BuyListingFooter
							isBusy={isBusy}
							canConfirm={canConfirm}
							onClose={onClose}
							onConfirm={onConfirm}
						/>
					</>
				)}
			</section>
		</Modal>
	);
}
