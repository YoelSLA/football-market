import { useState } from "react";
import genericPlayerImage from "@/shared/assets/Player Generic-Icon.png";
import styles from "./PortfolioPortrait.module.scss";

interface Props {
	imageUrl: string | null;
	name: string | null;
}

export default function PortfolioPortrait({ imageUrl, name }: Props) {
	const [failedUrl, setFailedUrl] = useState<string | null>(null);

	const src =
		imageUrl && imageUrl !== failedUrl ? imageUrl : genericPlayerImage;

	return (
		<img
			className={styles.portfolioPortrait}
			src={src}
			alt={name ? `Retrato de ${name}` : "Avatar genérico de jugador"}
			loading="lazy"
			onError={() => {
				if (imageUrl && src === imageUrl) {
					setFailedUrl(imageUrl);
				}
			}}
		/>
	);
}
