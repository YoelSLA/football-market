import genericPlayerImage from "@/shared/assets/Player Generic-Icon.png";
import styles from "./RankingPlacePortrait.module.scss";

interface Props {
	src?: string | null;
	name: string;
}

export default function RankingPlacePortrait({ src, name }: Props) {
	return (
		<div className={styles.portrait}>
			<img
				src={src || genericPlayerImage}
				alt={name}
				className={styles.portrait__image}
				loading="lazy"
				onError={(event) => {
					event.currentTarget.onerror = null;
					event.currentTarget.src = genericPlayerImage;
				}}
			/>
		</div>
	);
}
