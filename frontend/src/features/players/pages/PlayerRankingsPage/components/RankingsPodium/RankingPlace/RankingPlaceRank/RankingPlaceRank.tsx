import rank1Icon from "@/features/players/assets/rank-1.svg";
import rank2Icon from "@/features/players/assets/rank-2.svg";
import rank3Icon from "@/features/players/assets/rank-3.svg";
import styles from "./RankingPlaceRank.module.scss";

interface Props {
	rank: 1 | 2 | 3;
}

const RANK_ICONS = {
	1: rank1Icon,
	2: rank2Icon,
	3: rank3Icon,
} as const;

export default function RankingPlaceRank({ rank }: Props) {
	return (
		<div className={styles.rank}>
			<img
				src={RANK_ICONS[rank]}
				alt=""
				aria-hidden="true"
				className={styles.rank__icon}
			/>
		</div>
	);
}
