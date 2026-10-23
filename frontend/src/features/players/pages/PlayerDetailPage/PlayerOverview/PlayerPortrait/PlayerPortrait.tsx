import { useState } from "react";
import { DEMO_MESSAGES } from "@/features/players/constants";
import genericPlayerImage from "@/shared/assets/Player Generic-Icon.png";
import styles from "./PlayerPortrait.module.scss";

interface Props {
	name: string;
	imageUrl: string | null;
	fallbackImageUrl: string | null;
	className?: string;
}

/**
 * Retrato del jugador con el orden acordado: retrato principal, retrato alternativo y
 * finalmente icono genérico. No consulta proveedores ni inventa imágenes.
 */
export default function PlayerPortrait({
	name,
	imageUrl,
	fallbackImageUrl,
	className,
}: Props) {
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
			className={`${styles.playerPortrait} ${className ?? ""}`.trim()}
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
