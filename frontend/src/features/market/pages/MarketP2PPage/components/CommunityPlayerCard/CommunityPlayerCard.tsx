import type {
	CommunityMarketPlayer,
	MarketQuote,
} from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { PlayerPositionBadge } from "@/features/players";
import { Button, PlayerCardShell } from "@/shared/components";
import { TOKEN_SYMBOL } from "@/shared/constants";
import P2POpportunityBadge from "../P2POpportunityBadge";
import styles from "./CommunityPlayerCard.module.scss";

interface Props {
	player: CommunityMarketPlayer;
	officialQuote?: MarketQuote | null;
	onOpenPlayer: (player: CommunityMarketPlayer) => void;
}

export default function CommunityPlayerCard({
	player,
	officialQuote,
	onOpenPlayer,
}: Props) {
	const headerRight = officialQuote ? (
		<P2POpportunityBadge
			unitPrice={player.minimumUnitPrice}
			officialPrice={officialQuote.price}
		/>
	) : null;

	const headerMeta = (
		<div className={styles.priceBlock}>
			<span className={styles.priceLabel}>Mejor precio</span>
			<strong className={styles.priceValue}>
				{formatCredits(player.minimumUnitPrice)}
			</strong>
		</div>
	);

	const metricsStrip = (
		<div className={styles.liquidityBar}>
			<span className={styles.liquidityItem}>
				📦 {player.listingCount} {player.listingCount === 1 ? "lote" : "lotes"}
			</span>
			<span className={styles.liquiditySeparator} aria-hidden="true">
				·
			</span>
			<span className={styles.liquidityItem}>
				🪙 {player.availableQuantity} {TOKEN_SYMBOL} disp.
			</span>
		</div>
	);

	const actions = (
		<Button
			type="button"
			className={styles.actionButton}
			onClick={() => onOpenPlayer(player)}
			aria-label={`Ver ofertas de ${player.name}`}
		>
			<span>Ver ofertas</span>
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
			onNameClick={() => onOpenPlayer(player)}
		/>
	);
}
