import HotDemandBadge from "@/features/market/components/HotDemandBadge";
import { useHotPlayerIds } from "@/features/market/hooks";
import type { MarketListing, MarketQuote } from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { Button, TokenSymbol } from "@/shared/components";
import { Clock, Coins } from "@/shared/icons";
import P2POpportunityBadge from "../P2POpportunityBadge";
import styles from "./CommunityListingCard.module.scss";

interface Props {
	listing: MarketListing;
	officialQuote?: MarketQuote | null;
	onBuy: (listing: MarketListing, triggerElement: HTMLElement) => void;
}

function remaining(expiresAt: string): string {
	const ms = Date.parse(expiresAt) - Date.now();
	if (ms <= 0) return "Vencida";
	const totalMin = Math.floor(ms / (1000 * 60));
	const days = Math.floor(totalMin / (60 * 24));
	const hours = Math.floor((totalMin % (60 * 24)) / 60);
	const mins = totalMin % 60;
	if (days > 0) return `${days}d ${hours}h`;
	if (hours > 0) return `${hours}h ${mins}m`;
	return `${mins}m`;
}

export function CommunityListingCard({ listing, officialQuote, onBuy }: Props) {
	const { hotPlayerIds } = useHotPlayerIds();
	const isHot = hotPlayerIds.has(listing.playerId);
	const expired = Date.parse(listing.expiresAt) <= Date.now();

	return (
		<li className={styles.lotCard}>
			<article className={styles.lotCard__content}>
				<div className={styles.lotCard__header}>
					<div className={styles.lotCard__idPill}>
						<span>Lote</span>
						<h3>#{listing.listingId}</h3>
					</div>
					<div className={styles.lotCard__headerBadges}>
						{isHot && <HotDemandBadge />}
						{officialQuote && (
							<P2POpportunityBadge
								unitPrice={listing.unitPrice}
								officialPrice={officialQuote.price}
							/>
						)}
					</div>
				</div>

				<div className={styles.lotCard__metricsBox}>
					<div className={styles.lotCard__tokenBadge}>
						<div className={styles.lotCard__tokenIconWrap}>
							<Coins className={styles.lotCard__coinsIcon} aria-hidden="true" />
						</div>
						<span className={styles.lotCard__tokenCount}>
							{listing.quantity} <TokenSymbol />
						</span>
					</div>

					<div className={styles.lotCard__unitPriceBox}>
						<strong className={styles.lotCard__unitPrice}>
							{formatCredits(listing.unitPrice)}
						</strong>
						<span className={styles.lotCard__unitPriceLabel}>
							/ <TokenSymbol />
						</span>
					</div>
				</div>

				<div className={styles.lotCard__expiryRow}>
					<span className={styles.lotCard__expiryLabel}>
						<Clock className={styles.lotCard__clockIcon} aria-hidden="true" />
						<span>Vence en:</span>
					</span>
					<time
						className={styles.lotCard__expiryTime}
						dateTime={listing.expiresAt}
					>
						{remaining(listing.expiresAt)}
					</time>
				</div>

				<div className={styles.lotCard__footer}>
					<div className={styles.lotCard__totalRow}>
						<span className={styles.lotCard__totalLabel}>
							Precio total del lote
						</span>
						<strong className={styles.lotCard__totalAmount}>
							{formatCredits(listing.totalAmount)}
						</strong>
					</div>

					<Button
						className={styles.lotCard__buyButton}
						disabled={expired}
						aria-disabled={expired}
						onClick={(event) => onBuy(listing, event.currentTarget)}
					>
						<span>{expired ? "Oferta vencida" : "Comprar lote"}</span>
						{!expired && (
							<span className={styles.lotCard__arrow} aria-hidden="true">
								→
							</span>
						)}
					</Button>
				</div>
			</article>
		</li>
	);
}
