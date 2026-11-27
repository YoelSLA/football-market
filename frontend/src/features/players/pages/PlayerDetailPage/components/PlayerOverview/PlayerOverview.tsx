import type {
	PlayerDetail,
	PlayerQuote,
	PlayerStatistics,
} from "@/features/players/types";
import type { PortfolioPosition } from "@/features/portfolio/types";
import PlayerHoldingStrip from "../PlayerHoldingStrip";
import PlayerOverviewIdentity from "./components/PlayerOverviewIdentity";
import PlayerOverviewQuote from "./components/PlayerOverviewQuote";
import styles from "./PlayerOverview.module.scss";

interface Props {
	player: PlayerDetail;
	statistics: PlayerStatistics | undefined;
	quote?: PlayerQuote | null;
	quoteLoading?: boolean;
	isHot?: boolean;
	holding?: PortfolioPosition | null;
	availableCredits?: number;
	onBuyClick?: () => void;
	onSellClick?: () => void;
}

export default function PlayerOverview({
	player,
	statistics,
	quote,
	quoteLoading,
	isHot,
	holding,
	availableCredits,
	onBuyClick,
	onSellClick,
}: Props) {
	return (
		<section
			className={styles.playerOverview}
			aria-label="Identidad y cotización oficial del jugador"
		>
			<div className={styles.playerOverview__mainRow}>
				<PlayerOverviewIdentity
					name={player.name}
					imageUrl={player.imageUrl}
					fallbackImageUrl={player.fallbackImageUrl}
					teamName={player.teamName}
					leagueName={player.leagueName}
					position={player.position}
					nationality={player.nationality}
					dateOfBirth={player.dateOfBirth}
					isHot={isHot}
					statistics={statistics}
				/>

				<PlayerOverviewQuote
					quote={quote}
					loading={quoteLoading}
					className={styles.playerOverview__quoteContext}
					onBuyClick={onBuyClick}
					onSellClick={onSellClick}
				/>
			</div>

			<PlayerHoldingStrip
				holding={holding ?? null}
				availableCredits={availableCredits}
				playerName={player.name}
				currentPrice={quote?.price}
			/>
		</section>
	);
}
