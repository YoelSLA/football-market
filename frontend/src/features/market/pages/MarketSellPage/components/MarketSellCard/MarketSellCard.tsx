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
	return (
		<article className={styles.sellCard}>
			<MarketPlayerSummary player={player} />
			<div className={styles.sellCard__amounts}>
				<span>
					Total<strong>{player.totalQuantity}</strong>
				</span>
				<span>
					Disponibles<strong>{player.availableQuantity}</strong>
				</span>
				<span>
					Reservados<strong>{player.reservedQuantity}</strong>
				</span>
			</div>
			<div className={styles.sellCard__offer}>
				{quoteState.status === "available" && quoteState.quote ? (
					<p>
						<span>Precio actual de referencia</span>
						<strong>{formatCredits(quoteState.quote.price)}</strong>
					</p>
				) : (
					<p className={styles.sellCard__quoteStatus}>
						{quoteState.status === "missing"
							? "Cotización no disponible"
							: "Cotización pendiente"}
					</p>
				)}
				<Button
					disabled={player.availableQuantity <= 0}
					aria-label={`Vender tokens de ${player.name}`}
					onClick={(event) => onSell(player, event.currentTarget)}
				>
					<Tag aria-hidden="true" /> Publicar venta
				</Button>
			</div>
		</article>
	);
}
