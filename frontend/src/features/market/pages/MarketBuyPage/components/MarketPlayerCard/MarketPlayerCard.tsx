import type { OfficialMarketPlayer } from "@/features/market/types";
import {
	formatCredits,
	formatQuoteEffectiveDate,
	formatVariationPercentage,
	getVariationDirection,
} from "@/features/market/utils";
import { PlayerPositionBadge } from "@/features/players";
import { Button, PlayerCardShell } from "@/shared/components";
import { CalendarDays, ShoppingCart } from "@/shared/icons";
import styles from "./MarketPlayerCard.module.scss";

interface Props {
	player: OfficialMarketPlayer;
	onBuyClick: (
		player: OfficialMarketPlayer,
		triggerEl: HTMLElement | null,
	) => void;
}

export default function MarketPlayerCard({ player, onBuyClick }: Props) {
	const quote = player.quote;
	const direction = quote
		? getVariationDirection(quote.variationPercentage)
		: "neutral";
	const symbol =
		direction === "positive" ? "↑" : direction === "negative" ? "↓" : "→";

	const headerRight = quote ? (
		<div
			className={`${styles.variationBadge} ${styles[`variation_${direction}`]}`}
		>
			<span aria-hidden="true">{symbol}</span>
			<span>{formatVariationPercentage(quote.variationPercentage)}</span>
		</div>
	) : (
		<div className={styles.missingBadge}>
			<span>Sin cotización</span>
		</div>
	);

	const headerMeta = (
		<div className={styles.priceBlock}>
			<span className={styles.priceLabel}>Cotización</span>
			<strong className={styles.priceValue}>
				{quote ? formatCredits(quote.price, true) : "—"}
			</strong>
			<span className={styles.priceSubtext}>
				{quote
					? quote.previousPrice != null
						? `Ant: ${formatCredits(quote.previousPrice, true)}`
						: "Precio vigente"
					: "Sin cotización"}
			</span>
		</div>
	);

	const metricsStrip = (
		<div className={styles.subBar}>
			{quote ? (
				<>
					<CalendarDays aria-hidden="true" />
					<span>
						Vigente desde:{" "}
						<strong>{formatQuoteEffectiveDate(quote.effectiveDate)}</strong>
					</span>
				</>
			) : (
				<span className={styles.missingSubtext}>No hay cotización vigente</span>
			)}
		</div>
	);

	const actions = (
		<Button
			type="button"
			className={styles.buyButton}
			disabled={!quote}
			onClick={(event) => onBuyClick(player, event.currentTarget)}
			aria-label={`Comprar tokens de ${player.name}`}
		>
			<ShoppingCart aria-hidden="true" />
			<span>Comprar tokens</span>
			<span className={styles.actionArrow} aria-hidden="true">
				→
			</span>
		</Button>
	);

	return (
		<PlayerCardShell
			player={player}
			positionBadge={<PlayerPositionBadge position={player.position} />}
			headerRight={headerRight}
			headerMeta={headerMeta}
			metricsStrip={metricsStrip}
			actions={actions}
		/>
	);
}
