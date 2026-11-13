import type {
	PlayerDetail,
	PlayerQuote,
	PlayerStatistics,
} from "@/features/players/types";
import PlayerOverviewHeadlineMetrics from "./components/PlayerOverviewHeadlineMetrics";
import PlayerOverviewIdentity from "./components/PlayerOverviewIdentity";
import PlayerOverviewQuote from "./components/PlayerOverviewQuote";
import styles from "./PlayerOverview.module.scss";

interface Props {
	player: PlayerDetail;
	statistics: PlayerStatistics | undefined;
	quote?: PlayerQuote | null;
	quoteLoading?: boolean;
}

export default function PlayerOverview({
	player,
	statistics,
	quote,
	quoteLoading,
}: Props) {
	return (
		<section
			className={styles.playerOverview}
			aria-label="Identidad y cotización oficial del jugador"
		>
			<PlayerOverviewIdentity
				name={player.name}
				imageUrl={player.imageUrl}
				fallbackImageUrl={player.fallbackImageUrl}
				teamName={player.teamName}
				leagueName={player.leagueName}
				position={player.position}
				nationality={player.nationality}
				dateOfBirth={player.dateOfBirth}
			/>

			<PlayerOverviewHeadlineMetrics
				className={styles.playerOverview__headlineMetrics}
				statistics={statistics}
				position={player.position}
			/>

			<PlayerOverviewQuote
				quote={quote}
				loading={quoteLoading}
				className={styles.playerOverview__quoteContext}
			/>
		</section>
	);
}
