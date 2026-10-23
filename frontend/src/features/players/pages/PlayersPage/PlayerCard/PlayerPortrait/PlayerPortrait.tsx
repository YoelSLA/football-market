import { useState } from "react";
import type { Player } from "@/features/players/types";
import genericPlayerImage from "@/shared/assets/Player Generic-Icon.png";
import styles from "./PlayerPortrait.module.scss";

interface Props {
	player: Player;
}

export default function PlayerPortrait({ player }: Props) {
	const [imageIndex, setImageIndex] = useState(0);
	const images = [player.imageUrl, player.fallbackImageUrl]
		.filter((url): url is string => Boolean(url))
		.filter((url, index, urls) => urls.indexOf(url) === index);
	images.push(genericPlayerImage);

	return (
		<img
			className={styles.playerPortrait}
			src={images[imageIndex]}
			alt=""
			loading="lazy"
			decoding="async"
			onError={() =>
				setImageIndex((index) => Math.min(index + 1, images.length - 1))
			}
		/>
	);
}
