import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import type { OfficialMarketPlayer } from "@/features/market/types";
import {
	formatCredits,
	formatQuoteEffectiveDate,
	formatVariationPercentage,
	getVariationDirection,
} from "@/features/market/utils";
import { Button } from "@/shared/components";
import { CalendarDays, ShoppingCart } from "@/shared/icons";
import styles from "./MarketPlayerCard.module.scss";

interface Props {
	player: OfficialMarketPlayer;
	onBuyClick: (
		player: OfficialMarketPlayer,
		triggerEl: HTMLElement | null,
	) => void;
}

function MarketQuoteSummary({
	quote,
}: {
	quote: NonNullable<OfficialMarketPlayer["quote"]>;
}) {
	const direction = getVariationDirection(quote.variationPercentage);
	const symbol =
		direction === "positive" ? "↑" : direction === "negative" ? "↓" : "→";

	return (
		<div className={styles.card__quote}>
			<div className={styles.card__metrics}>
				<span className={styles.card__label}>Cotización actual</span>
				<span className={styles.card__label}>Variación</span>
				<span className={styles.card__label}>Vigente desde</span>

				<strong className={styles.card__price}>
					{formatCredits(quote.price, true)}
				</strong>
				<span
					className={`${styles.card__variation} ${
						styles[`card__variation_${direction}`]
					}`}
				>
					<span aria-hidden="true">{symbol}</span>
					{formatVariationPercentage(quote.variationPercentage)}
				</span>
				<div className={styles.card__effectiveDate}>
					<CalendarDays aria-hidden="true" />
					<span>{formatQuoteEffectiveDate(quote.effectiveDate)}</span>
				</div>

				<span className={styles.card__previousPrice}>
					Anterior: {formatCredits(quote.previousPrice, true)}
				</span>
				<span className={styles.card__variationCaption}>
					Respecto al anterior
				</span>
			</div>
		</div>
	);
}

function MarketQuoteUnavailable() {
	return (
		<div className={styles.card__missingAlert} role="status">
			<strong>Sin cotización</strong>

			<p className={styles.card__missingText}>
				No existe una cotización vigente para este jugador.
			</p>
		</div>
	);
}

export default function MarketPlayerCard({ player, onBuyClick }: Props) {
	const quote = player.quote;

	return (
		<article className={styles.card}>
			<div className={styles.card__identity}>
				<MarketPlayerSummary player={player} variant="officialCard" />
			</div>

			<div className={styles.card__content}>
				{quote ? (
					<MarketQuoteSummary quote={quote} />
				) : (
					<MarketQuoteUnavailable />
				)}
			</div>

			<div className={styles.card__actions}>
				<Button
					type="button"
					className={styles.card__buyButton}
					disabled={!quote}
					onClick={(event) => onBuyClick(player, event.currentTarget)}
					aria-label={`Comprar tokens de ${player.name}`}
				>
					<ShoppingCart aria-hidden="true" />
					Comprar tokens
				</Button>
			</div>
		</article>
	);
}
