import { useMemo, useState } from "react";
import genericPlayerImage from "@/shared/assets/Player Generic-Icon.png";
import styles from "./PlayerPortrait.module.scss";

export interface PlayerPortraitProps {
	/** URL principal de la imagen del jugador */
	src?: string | null;
	/** URL alternativa/fallback antes del icono genérico */
	fallbackSrc?: string | null;
	/** Nombre del jugador para accesibilidad */
	name?: string | null;
	/** Texto alternativo explícito. Si se omite, se deriva de `name` */
	alt?: string;
	/** Ajuste del objeto de imagen: 'cover' (por defecto) o 'contain' */
	fit?: "cover" | "contain";
	/** Tamaño estándar predefinido */
	size?: "sm" | "md" | "lg" | "xl";
	/** Clases CSS adicionales */
	className?: string;
	/** Estrategia de carga */
	loading?: "lazy" | "eager";
	/** Modo de decodificación */
	decoding?: "async" | "auto" | "sync";
}

export default function PlayerPortrait({
	src,
	fallbackSrc,
	name,
	alt,
	fit = "cover",
	size,
	className,
	loading = "lazy",
	decoding = "async",
}: PlayerPortraitProps) {
	const candidates = useMemo(() => {
		const list = [src, fallbackSrc]
			.filter((url): url is string => Boolean(url?.trim()))
			.filter((url, index, all) => all.indexOf(url) === index);
		list.push(genericPlayerImage);
		return list;
	}, [src, fallbackSrc]);

	const [failedCount, setFailedCount] = useState(0);
	const [prevSources, setPrevSources] = useState({ src, fallbackSrc });

	if (prevSources.src !== src || prevSources.fallbackSrc !== fallbackSrc) {
		setPrevSources({ src, fallbackSrc });
		setFailedCount(0);
	}

	const candidateIndex = Math.min(failedCount, candidates.length - 1);
	const currentSrc = candidates[candidateIndex] ?? genericPlayerImage;
	const isGeneric = currentSrc === genericPlayerImage;

	const resolvedAlt =
		alt !== undefined
			? alt
			: isGeneric
				? name
					? `Avatar genérico de ${name}`
					: "Avatar genérico de jugador"
				: name
					? `Retrato de ${name}`
					: "Retrato de jugador";

	const classNames = [
		styles.playerPortrait,
		styles[`playerPortrait_${fit}`],
		size && styles[`playerPortrait_${size}`],
		className,
	]
		.filter(Boolean)
		.join(" ");

	return (
		<img
			className={classNames}
			src={currentSrc}
			alt={resolvedAlt}
			loading={loading}
			decoding={decoding}
			onError={() => {
				setFailedCount((prev) => Math.min(prev + 1, candidates.length - 1));
			}}
		/>
	);
}
