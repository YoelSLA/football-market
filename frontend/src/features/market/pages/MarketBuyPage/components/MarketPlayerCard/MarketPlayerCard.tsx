import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import type { OfficialMarketPlayer } from "@/features/market/types";
import {
	formatCredits,
	formatQuoteEffectiveDate,
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

const percentageFormatter = new Intl.NumberFormat("es-AR", {
	minimumFractionDigits: 1,
	maximumFractionDigits: 2,
});

export default function MarketPlayerCard({ player, onBuyClick }: Props) {
	const quote = player.quote;
	const variation = quote ? quote.variationPercentage : null;
	const direction =
		variation === null || variation === undefined || variation === 0
			? "neutral"
			: variation > 0
				? "positive"
				: "negative";
	const variationSymbol =
		direction === "positive" ? "↑" : direction === "negative" ? "↓" : "→";
	return (
		<article className={styles.card}>
			<MarketPlayerSummary player={player} variant="officialCard" />
			<div className={styles.card__content}>
				{quote ? (
					<>
						<div className={styles.card__metrics}>
							<div className={styles.card__metric}>
								<span className={styles.card__label}>Cotización actual</span>
								<strong className={styles.card__price}>
									{formatCredits(quote.price, true)}
								</strong>
							</div>
							<div className={styles.card__metric}>
								<span className={styles.card__label}>Variación</span>
								<span
									className={`${styles.card__variation} ${styles[`card__variation_${direction}`]}`}
								>
									<span aria-hidden="true">{variationSymbol}</span>
									{variation === null
										? "Sin referencia"
										: `${variation > 0 ? "+" : ""}${percentageFormatter.format(variation)} %`}
								</span>
							</div>
						</div>
						<div className={styles.card__secondaryData}>
							<span>Anterior: {formatCredits(quote.previousPrice, true)}</span>
							<span>
								<CalendarDays aria-hidden="true" /> Vigente:{" "}
								{formatQuoteEffectiveDate(quote.effectiveDate)}
							</span>
						</div>
					</>
				) : (
					<div className={styles.card__missingAlert} role="status">
						<strong>Sin cotización</strong>
						<p className={styles.card__missingText}>
							No existe una cotización vigente para este jugador.
						</p>
					</div>
				)}
			</div>
			<div className={styles.card__actions}>
				<Button
					className={
						quote ? styles.card__buyButton : styles.card__buyButtonDisabled
					}
					disabled={!quote}
					onClick={(event) => onBuyClick(player, event.currentTarget)}
					aria-label={`Comprar tokens de ${player.name}`}
				>
					<ShoppingCart aria-hidden="true" /> Comprar
				</Button>
			</div>
		</article>
	);
}
