import PlayerHeroCard from "@/features/market/components/PlayerHeroCard";
import type {
	OfficialMarketPlayer,
	OfficialMarketQuote,
} from "@/features/market/types";
import {
	formatCentimes,
	formatCredits,
	formatQuoteEffectiveDate,
	formatVariationPercentage,
} from "@/features/market/utils";
import styles from "./BuyModalHeroCard.module.scss";

interface Props {
	player: OfficialMarketPlayer;
	quote: OfficialMarketQuote;
}

export default function BuyModalHeroCard({ player, quote }: Props) {
	const isGain =
		quote.variationPercentage != null && quote.variationPercentage >= 0;

	const headerRight = (
		<div className={styles.holdingBadge}>
			<span className={styles.pillOfficial}>⚡ Emisión Oficial</span>
		</div>
	);

	const statBoxLeft = (
		<>
			<span className={styles.statLabel}>Cotización Ref.</span>
			<div className={styles.statValRow}>
				<strong className={styles.statValue}>
					{formatCredits(quote.price, true)}
				</strong>
				{quote.variationPercentage != null && (
					<span
						className={`${styles.variationBadge} ${
							isGain ? styles.variationBadge_gain : styles.variationBadge_loss
						}`}
					>
						{isGain ? "↑ " : "↓ "}
						{formatVariationPercentage(quote.variationPercentage)}
					</span>
				)}
			</div>
			<span className={styles.statSub}>
				{quote.previousPrice != null
					? `Ant: ${formatCredits(quote.previousPrice, true)} · ${formatCentimes(quote.price)}`
					: `${formatCentimes(quote.price)} · Cotización inicial`}
			</span>
		</>
	);

	const statBoxRight = (
		<>
			<span className={styles.statLabel}>Vigencia</span>
			<div className={styles.statValRow}>
				<strong className={styles.statValue}>
					{formatQuoteEffectiveDate(quote.effectiveDate)}
				</strong>
			</div>
			<span className={styles.statSub}>Precio oficial garantizado</span>
		</>
	);

	return (
		<PlayerHeroCard
			player={player}
			headerRight={headerRight}
			statBoxLeft={statBoxLeft}
			statBoxRight={statBoxRight}
		/>
	);
}
