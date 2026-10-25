import type { MarketPlayer, MarketQuote, MarketQuoteStatus } from "@/features/market/types";
import {
	formatCentimes,
	formatCredits,
	formatQuoteCalculatedAt,
} from "@/features/market/utils";
import { Button } from "@/shared/components";
import { ShoppingCart } from "@/shared/icons";
import MarketPlayerSummary from "../MarketPlayerSummary";
import MarketQuoteFeedback from "../MarketQuoteFeedback";
import styles from "./MarketPlayerCard.module.scss";

interface Props {
	player: MarketPlayer;
	quote: MarketQuote | null;
	quoteStatus: MarketQuoteStatus;
	quoteHttpStatus?: number;
	canRetryQuote: boolean;
	onRetryQuote: () => void;
	onBuyClick: (player: MarketPlayer, triggerEl: HTMLElement | null) => void;
}

/**
 * Ficha local de jugador en el mercado.
 * Presenta datos de catálogo, cotización vigente calculada y acción de compra.
 * Recibe todos sus datos y callbacks exclusivamente por props.
 */
export default function MarketPlayerCard({
	player,
	quote,
	quoteStatus,
	quoteHttpStatus,
	canRetryQuote,
	onRetryQuote,
	onBuyClick,
}: Props) {
	return (
		<article className={styles.card}>
			<MarketPlayerSummary player={player} />
			<div className={styles.card__body}>
				<div className={styles.card__quote}>
					<p className={styles.card__label}>Precio actual</p>
					<MarketQuoteFeedback status={quoteStatus} httpStatus={quoteHttpStatus} canRetry={canRetryQuote} onRetry={onRetryQuote} />
					{quoteStatus === "available" && quote && (
						<>
							<div className={styles.card__priceRow}>
								<span className={styles.card__priceCredits}>
									{formatCredits(quote.price)}
								</span>
								<span className={styles.card__priceCentimes}>
									({formatCentimes(quote.price)})
								</span>
							</div>
							<p className={styles.card__calculatedAt} title={formatQuoteCalculatedAt(quote.calculatedAt)}>
								{formatQuoteCalculatedAt(quote.calculatedAt)}
							</p>
						</>
					)}
				</div>
				<div className={styles.card__variation}><p className={styles.card__label}>Variación (24h)</p><span>Sin datos</span></div>
			</div>

			<div className={styles.card__actions}>
				<Button
					className={styles.card__buyButton}
					aria-label={`Comprar tokens de ${player.name}`}
					onClick={(e) => onBuyClick(player, e.currentTarget)}
				>
					<ShoppingCart aria-hidden="true" /> Comprar
				</Button>
			</div>
		</article>
	);
}
