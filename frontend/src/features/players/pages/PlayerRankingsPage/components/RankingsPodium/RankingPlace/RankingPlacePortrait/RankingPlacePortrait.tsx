import { PlayerPortrait } from "@/shared/components";
import styles from "./RankingPlacePortrait.module.scss";

interface Props {
	src?: string | null;
	name: string;
}

export default function RankingPlacePortrait({ src, name }: Props) {
	return (
		<div className={styles.portrait}>
			<PlayerPortrait
				src={src}
				name={name}
				className={styles.portrait__image}
			/>
		</div>
	);
}
