import type { PlayerRankingItem, RankingMode } from "@/features/players/types";
import RankingPlace from "./RankingPlace";
import styles from "./RankingsPodium.module.scss";

interface Props {
	topPlayers: PlayerRankingItem[];
	mode: RankingMode;
}

/**
 * Organiza los puestos devueltos por la consulta independiente del podio.
 *
 * Orden visual:
 * - Segundo puesto: izquierda.
 * - Primer puesto: centro.
 * - Tercer puesto: derecha.
 *
 * Si la consulta devuelve menos de tres entradas, solo se representan las disponibles.
 */
export default function RankingsPodium({ topPlayers, mode }: Props) {
	if (!topPlayers.length) return null;

	const slots = [
		{
			rank: 2 as const,
			player: topPlayers.find((item) => item.position === 2),
		},
		{
			rank: 1 as const,
			player: topPlayers.find((item) => item.position === 1),
		},
		{
			rank: 3 as const,
			player: topPlayers.find((item) => item.position === 3),
		},
	];

	return (
		<section
			className={styles.podiumSection}
			aria-label="Podio de líderes Top 3"
		>
			<div className={styles.podiumGrid}>
				{slots.map(({ rank, player }) =>
					player ? (
						<div
							key={rank}
							className={`${styles.podiumColumn} ${
								rank === 1 ? styles.podiumColumn_center : ""
							}`}
						>
							<RankingPlace player={player} rank={rank} mode={mode} />
						</div>
					) : null,
				)}
			</div>
		</section>
	);
}
