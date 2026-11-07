import styles from "./TableMedal.module.scss";

interface Props {
	rank: 1 | 2 | 3;
}

const RANK_MODIFIERS = {
	1: styles.tableMedal_rank1,
	2: styles.tableMedal_rank2,
	3: styles.tableMedal_rank3,
} as const;

export default function TableMedal({ rank }: Props) {
	return (
		<svg
			className={`${styles.tableMedal} ${RANK_MODIFIERS[rank]}`}
			width="22"
			height="26"
			viewBox="0 0 24 28"
			fill="none"
			xmlns="http://www.w3.org/2000/svg"
			aria-hidden="true"
		>
			{/* Cintas de la medalla */}
			<path
				className={styles.tableMedal__ribbon}
				d="M8 15L5 25L9 22L12 25L10 16"
			/>
			<path
				className={styles.tableMedal__ribbon}
				d="M16 15L19 25L15 22L12 25L14 16"
			/>

			{/* Círculo de la medalla */}
			<circle className={styles.tableMedal__circle} cx="12" cy="11" r="9" />

			{/* Anillo interior brillante */}
			<circle
				className={styles.tableMedal__ring}
				cx="12"
				cy="11"
				r="7.5"
				strokeWidth="1"
				fill="none"
			/>

			{/* Número del puesto */}
			<text
				className={styles.tableMedal__text}
				x="12"
				y="12"
				textAnchor="middle"
				dominantBaseline="central"
			>
				{rank}
			</text>
		</svg>
	);
}
