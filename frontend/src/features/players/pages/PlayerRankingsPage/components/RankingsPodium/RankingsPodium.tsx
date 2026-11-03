import type { PlayerRankingItem, RankingMode } from "@/features/players/types";
import RankingPlace from "./RankingPlace";
import styles from "./RankingsPodium.module.scss";

interface Props {
	topPlayers: PlayerRankingItem[];
	mode: RankingMode;
}

/**
 * Organiza los tres primeros puestos del ranking.
 *
 * Orden visual:
 * - Segundo puesto: izquierda.
 * - Primer puesto: centro.
 * - Tercer puesto: derecha.
 */
export default function RankingsPodium({ topPlayers, mode }: Props) {
	if (!topPlayers.length) return null;

	const rank1 = topPlayers.find((player) => player.position === 1);
	const rank2 = topPlayers.find((player) => player.position === 2);
	const rank3 = topPlayers.find((player) => player.position === 3);

	return (
		<section
			className={styles.podiumSection}
			aria-label="Podio de líderes Top 3"
		>
			<div className={styles.podiumGrid}>
				<div className={styles.podiumColumn}>
					<RankingPlace player={rank2} rank={2} mode={mode} />
				</div>

				<div className={`${styles.podiumColumn} ${styles.podiumColumn_center}`}>
					<RankingPlace player={rank1} rank={1} mode={mode} />
				</div>

				<div className={styles.podiumColumn}>
					<RankingPlace player={rank3} rank={3} mode={mode} />
				</div>
			</div>
		</section>
	);
}
