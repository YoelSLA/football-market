import { useNavigate } from "react-router-dom";
import type { MarketPlayerQuoteState } from "@/features/market/hooks";
import { formatCredits } from "@/features/market/utils";
import { PlayerPositionBadge } from "@/features/players";
import { Button, PlayerCardShell, TokenSymbol } from "@/shared/components";
import { TOKEN_SYMBOL } from "@/shared/constants";
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

	const headerRight = (
		<div className={styles.holdingBadge}>
			{isFullyReserved ? (
				<span className={styles.badgeReserved}>🔒 100% En orden</span>
			) : hasReservedTokens ? (
				<span className={styles.badgeMixed}>
					🪙 {player.availableQuantity} <TokenSymbol size="xs" /> Libres ·{" "}
					{player.reservedQuantity} en Venta
				</span>
			) : (
				<span className={styles.badgeAvailable}>
					🪙 {player.availableQuantity} <TokenSymbol size="xs" />{" "}
					{player.availableQuantity === 1 ? "Libre" : "Libres"}
				</span>
			)}
		</div>
	);

	const headerMeta = (
		<div className={styles.priceBlock}>
			<span className={styles.priceLabel}>Cotización Ref.</span>
			<strong className={styles.priceValue}>
				{isQuoteAvailable ? formatCredits(quote.price, true) : "—"}
			</strong>
			<span className={styles.priceSubtext}>
				{isQuoteAvailable
					? quote.previousPrice != null
						? `Ant: ${formatCredits(quote.previousPrice, true)}`
						: "Precio vigente"
					: quoteState.status === "loading"
						? "Consultando…"
						: "Sin cotización"}
			</span>
		</div>
	);

	const metricsStrip = (
		<div className={styles.holdingBar}>
			<span
				className={`${styles.holdingItem} ${
					hasAvailableTokens
						? styles.holdingItem_available
						: styles.holdingItem_zero
				}`}
			>
				🪙 {player.availableQuantity} <TokenSymbol /> libres
			</span>
			<span className={styles.holdingSeparator} aria-hidden="true">
				·
			</span>
			<span
				className={`${styles.holdingItem} ${
					hasReservedTokens
						? styles.holdingItem_reserved
						: styles.holdingItem_muted
				}`}
			>
				{hasReservedTokens
					? `🔒 ${player.reservedQuantity} en venta`
					: "0 en venta"}
			</span>
		</div>
	);

	const actions = isFullyReserved ? (
		<Button
			type="button"
			className={styles.viewOrdersButton}
			aria-label={`Ver publicaciones activas de ${player.name} en mis órdenes`}
			onClick={handleViewOrders}
		>
			<Puzzle aria-hidden="true" />
			<span>Ver en mis órdenes</span>
			<span className={styles.actionArrow} aria-hidden="true">
				→
			</span>
		</Button>
	) : (
		<div
			className={hasReservedTokens ? styles.actionsRow : styles.actionsSingle}
		>
			<Button
				type="button"
				className={styles.sellButton}
				disabled={!hasAvailableTokens}
				aria-label={`Publicar venta de ${player.name}`}
				onClick={(event) => onSell(player, event.currentTarget)}
			>
				<Tag aria-hidden="true" />
				<span>Publicar venta</span>
				<span className={styles.actionArrow} aria-hidden="true">
					→
				</span>
			</Button>

			{hasReservedTokens && (
				<button
					type="button"
					className={styles.viewOrdersLink}
					title={`Ver ${player.reservedQuantity} ${TOKEN_SYMBOL} en orden activa en mis órdenes`}
					onClick={handleViewOrders}
				>
					<Puzzle aria-hidden="true" />
					<span>{player.reservedQuantity} en orden</span>
				</button>
			)}
		</div>
	);

	return (
		<PlayerCardShell
			player={player}
			frozen={isFullyReserved}
			positionBadge={<PlayerPositionBadge position={player.position} />}
			headerRight={headerRight}
			headerMeta={headerMeta}
			metricsStrip={metricsStrip}
			actions={actions}
		/>
	);
}
