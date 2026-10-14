import { useState } from "react";
import genericPlayerImage from "../../assets/Player Generic-Icon.png";
import { DEMO_MESSAGES } from "../../constants";
import styles from "./PlayerPortrait.module.scss";

interface PlayerPortraitProps {
	name: string;
	imageUrl: string | null;
	fallbackImageUrl: string | null;
	className?: string;
}

/**
 * Retrato del jugador con el orden acordado: retrato principal, retrato alternativo y
 * finalmente icono genérico. No consulta proveedores ni inventa imágenes.
 */
export function PlayerPortrait({
	name,
	imageUrl,
	fallbackImageUrl,
	className,
}: PlayerPortraitProps) {
	const [imageIndex, setImageIndex] = useState(0);

	const images = [imageUrl, fallbackImageUrl]
		.filter((url): url is string => Boolean(url))
		.filter((url, index, urls) => urls.indexOf(url) === index);

	const withGeneric = [...images, genericPlayerImage];
	const currentIndex = Math.min(imageIndex, withGeneric.length - 1);
	const currentImage = withGeneric[currentIndex];
	const isGeneric = currentIndex >= images.length;

	return (
		<img
			key={currentImage}
			className={`${styles["player-portrait"]} ${className ?? ""}`.trim()}
			src={currentImage}
			alt={
				isGeneric
					? DEMO_MESSAGES.genericPortraitAlt
					: DEMO_MESSAGES.playerPortraitAlt.replace("{name}", name)
			}
			onError={() =>
				setImageIndex((index) => Math.min(index + 1, withGeneric.length - 1))
			}
		/>
	);
}
