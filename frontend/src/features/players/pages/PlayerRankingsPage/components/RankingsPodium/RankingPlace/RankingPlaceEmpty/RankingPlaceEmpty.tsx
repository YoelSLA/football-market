import styles from "./RankingPlaceEmpty.module.scss";

interface Props {
	rank: 1 | 2 | 3;
}

export default function RankingPlaceEmpty({ rank }: Props) {
	return (
		<div className={styles.empty}>
			<span className={styles.empty__rank}>#{rank}</span>

			<span className={styles.empty__text}>Posición vacante</span>
		</div>
	);
}
