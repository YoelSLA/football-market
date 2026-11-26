import MarketQuoteFeedback from "@/features/market/components/MarketQuoteFeedback";
import PlayerHeroCard from "@/features/market/components/PlayerHeroCard";
import type {
	MarketPlayer,
	MarketQuote,
	MarketQuoteStatus,
} from "@/features/market/types";
import {
	formatCentimes,
	formatCredits,
	formatQuoteCalculatedAt,
} from "@/features/market/utils";
import { TokenSymbol } from "@/shared/components";
import styles from "./SellModalHeroCard.module.scss";

interface Props {
	player: MarketPlayer;
	quote: MarketQuote | null;
	quoteStatus: MarketQuoteStatus;
	quoteHttpStatus?: number;
	availableQuantity: number;
	reservedQuantity: number;
	canRetryQuote: boolean;
	onRetryQuote: () => void;
}

export default function SellModalHeroCard({
	player,
	quote,
	quoteStatus,
	quoteHttpStatus,
	availableQuantity,
	reservedQuantity,
	canRetryQuote,
	onRetryQuote,
}: Props) {
	const isQuoteAvailable = quoteStatus === "available" && quote !== null;
	const totalQuantity = availableQuantity + reservedQuantity;

	const hasPreviousPrice =
		isQuoteAvailable && quote.previousPrice != null && quote.previousPrice > 0;
	const priceDeltaPercent =
		hasPreviousPrice && quote
			? ((quote.price - (quote.previousPrice ?? 0)) /
					(quote.previousPrice ?? 1)) *
				100
			: null;

	const headerRight = (
		<div className={styles.holdingBadge}>
			{availableQuantity > 0 ? (
				<span className={styles.pillAvailable}>
					🪙 {availableQuantity} <TokenSymbol size="xs" />{" "}
					{availableQuantity === 1 ? "Libre" : "Libres"}
				</span>
			) : (
				<span className={styles.pillReserved}>
					🔒 {reservedQuantity} <TokenSymbol size="xs" /> en orden
				</span>
			)}
		</div>
	);

	const statBoxLeft = (
		<>
			<span className={styles.statLabel}>Cotización Ref.</span>
			<div className={styles.statValRow}>
				<strong className={styles.statValue}>
					{isQuoteAvailable ? formatCredits(quote.price, true) : "—"}
				</strong>
				{priceDeltaPercent !== null && (
					<span
						className={`${styles.variationBadge} ${
							priceDeltaPercent >= 0
								? styles.variationBadge_gain
								: styles.variationBadge_loss
						}`}
					>
						{priceDeltaPercent >= 0 ? "↑ +" : "↓ "}
						{priceDeltaPercent.toFixed(2)}%
					</span>
				)}
			</div>
			<span className={styles.statSub}>
				{isQuoteAvailable
					? quote.previousPrice != null
						? `Ant: ${formatCredits(quote.previousPrice, true)} · ${formatCentimes(quote.price)}`
						: `${formatCentimes(quote.price)} · ${formatQuoteCalculatedAt(quote.calculatedAt)}`
					: quoteStatus === "loading"
						? "Consultando…"
						: "No disponible"}
			</span>
		</>
	);

	const statBoxRight = (
		<>
			<span className={styles.statLabel}>Tus Tokens</span>
			<div className={styles.statValRow}>
				<strong
					className={`${styles.statValue} ${
						availableQuantity > 0 ? styles.statValue_available : ""
					}`}
				>
					{availableQuantity} <TokenSymbol size="sm" />
				</strong>
				<span className={styles.libresBadge}>Libres</span>
			</div>
			<span className={styles.statSub}>
				{reservedQuantity > 0
					? `${reservedQuantity} en venta · Total: ${totalQuantity}`
					: `Total en cartera: ${totalQuantity}`}
			</span>
		</>
	);

	const footerContent = (
		<MarketQuoteFeedback
			status={quoteStatus}
			httpStatus={quoteHttpStatus}
			canRetry={canRetryQuote}
			onRetry={onRetryQuote}
		/>
	);

	return (
		<PlayerHeroCard
			player={player}
			headerRight={headerRight}
			statBoxLeft={statBoxLeft}
			statBoxRight={statBoxRight}
			footerContent={footerContent}
		/>
	);
}
