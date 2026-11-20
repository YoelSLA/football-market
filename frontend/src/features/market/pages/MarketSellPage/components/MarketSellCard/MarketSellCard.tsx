import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import type { MarketPlayerQuoteState } from "@/features/market/hooks";
import { formatCredits } from "@/features/market/utils";
import { Button } from "@/shared/components";
import { Tag } from "@/shared/icons";
import type { PlayerHolding } from "../../MarketSellPage";
import styles from "./MarketSellCard.module.scss";

interface Props {
	player: PlayerHolding;
	quoteState: MarketPlayerQuoteState;
	onSell: (player: PlayerHolding, target: HTMLElement) => void;
}

export default function MarketSellCard({ player, quoteState, onSell }: Props) {
	const quote = quoteState.quote;
	const isQuoteAvailable = quoteState.status === "available" && quote !== null;
	const hasAvailableTokens = player.availableQuantity > 0;

	return (
		<article className={styles.sellCard}>
			<div className={styles.sellCard__identity}>
				<MarketPlayerSummary player={player} variant="officialCard" />
			</div>

			<div className={styles.sellCard__content}>
				<div className={styles.sellCard__metrics}>
					<div className={styles.sellCard__metric}>
						<span className={styles.sellCard__label}>Cotización ref.</span>
						<strong className={styles.sellCard__price}>
							{isQuoteAvailable ? formatCredits(quote.price, true) : "—"}
						</strong>
						<span className={styles.sellCard__subtext}>
							{isQuoteAvailable
								? quote.previousPrice != null
									? `Ant: ${formatCredits(quote.previousPrice, true)}`
									: "Precio vigente"
								: quoteState.status === "loading"
									? "Consultando…"
									: "Sin cotización"}
						</span>
					</div>

					<div className={styles.sellCard__metric}>
						<span className={styles.sellCard__label}>Disponibles</span>
						<strong
							className={`${styles.sellCard__quantity} ${
								hasAvailableTokens ? styles.sellCard__quantity_available : ""
							}`}
						>
							{player.availableQuantity}
						</strong>
						<span className={styles.sellCard__subtext}>
							{player.availableQuantity === 1 ? "token libre" : "tokens libres"}
						</span>
					</div>

					<div className={styles.sellCard__metric}>
						<span className={styles.sellCard__label}>En cartera</span>
						<strong className={styles.sellCard__quantity}>
							{player.totalQuantity}
						</strong>
						<span className={styles.sellCard__subtext}>
							{player.reservedQuantity} en reserva
						</span>
					</div>
				</div>
			</div>

			<div className={styles.sellCard__actions}>
				<Button
					type="button"
					className={styles.sellCard__sellButton}
					disabled={!hasAvailableTokens}
					aria-label={`Publicar venta de ${player.name}`}
					onClick={(event) => onSell(player, event.currentTarget)}
				>
					<Tag aria-hidden="true" />
					Publicar venta
				</Button>
			</div>
		</article>
	);
}
