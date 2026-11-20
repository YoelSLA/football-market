import { useNavigate } from "react-router-dom";
import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import type { MarketPlayerQuoteState } from "@/features/market/hooks";
import { formatCredits } from "@/features/market/utils";
import { Button } from "@/shared/components";
import { Puzzle, Tag } from "@/shared/icons";
import type { PlayerHolding } from "../../MarketSellPage";
import styles from "./MarketSellCard.module.scss";

interface Props {
	player: PlayerHolding;
	quoteState: MarketPlayerQuoteState;
	onSell: (player: PlayerHolding, target: HTMLElement) => void;
}

export default function MarketSellCard({ player, quoteState, onSell }: Props) {
	const navigate = useNavigate();
	const quote = quoteState.quote;
	const isQuoteAvailable = quoteState.status === "available" && quote !== null;
	const hasAvailableTokens = player.availableQuantity > 0;
	const hasReservedTokens = player.reservedQuantity > 0;
	const isFullyReserved = !hasAvailableTokens && hasReservedTokens;

	const handleViewOrders = () => {
		navigate(
			`/market/orders?search=${encodeURIComponent(player.name)}&status=ACTIVE`,
		);
	};

	return (
		<article
			className={`${styles.sellCard} ${
				isFullyReserved ? styles.sellCard_frozen : ""
			}`}
		>
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
						<span className={styles.sellCard__label}>Tokens libres</span>
						<strong
							className={`${styles.sellCard__quantity} ${
								hasAvailableTokens
									? styles.sellCard__quantity_available
									: styles.sellCard__quantity_zero
							}`}
						>
							{player.availableQuantity}
						</strong>
						<span
							className={`${styles.sellCard__subtext} ${
								!hasAvailableTokens ? styles.sellCard__subtext_zero : ""
							}`}
						>
							{hasAvailableTokens
								? player.availableQuantity === 1
									? "token libre"
									: "tokens libres"
								: "0 disponibles"}
						</span>
					</div>

					<div className={styles.sellCard__metric}>
						<span
							className={`${styles.sellCard__label} ${
								hasReservedTokens ? styles.sellCard__label_reserved : ""
							}`}
						>
							{hasReservedTokens ? "En venta" : "En cartera"}
						</span>
						<strong
							className={`${styles.sellCard__quantity} ${
								hasReservedTokens ? styles.sellCard__quantity_reserved : ""
							}`}
						>
							{hasReservedTokens
								? player.reservedQuantity
								: player.totalQuantity}
						</strong>
						<span className={styles.sellCard__subtext}>
							{hasReservedTokens
								? `Total: ${player.totalQuantity}`
								: "0 en venta"}
						</span>
					</div>
				</div>
			</div>

			<div className={styles.sellCard__actions}>
				{isFullyReserved ? (
					<Button
						type="button"
						className={styles.sellCard__viewOrdersButton}
						aria-label={`Ver publicaciones activas de ${player.name} en mis órdenes`}
						onClick={handleViewOrders}
					>
						<Puzzle aria-hidden="true" />
						Ver en mis órdenes
					</Button>
				) : (
					<div
						className={
							hasReservedTokens
								? styles.sellCard__actionsRow
								: styles.sellCard__actionsSingle
						}
					>
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

						{hasReservedTokens && (
							<button
								type="button"
								className={styles.sellCard__viewOrdersLink}
								title={`Ver ${player.reservedQuantity} tokens en orden activa en mis órdenes`}
								onClick={handleViewOrders}
							>
								<Puzzle aria-hidden="true" />
								<span>{player.reservedQuantity} en orden</span>
							</button>
						)}
					</div>
				)}
			</div>
		</article>
	);
}
