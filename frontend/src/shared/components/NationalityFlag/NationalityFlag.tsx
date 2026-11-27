import { useState } from "react";
import { getNationalityFlag } from "@/shared/utils/nationalityFlag";
import styles from "./NationalityFlag.module.scss";

export interface NationalityFlagProps {
	nationality?: string | null;
	src?: string | null;
	className?: string;
	loading?: "lazy" | "eager";
	decoding?: "async" | "auto" | "sync";
}

export default function NationalityFlag({
	nationality,
	src,
	className,
	loading = "lazy",
	decoding = "async",
}: NationalityFlagProps) {
	const [failedUrl, setFailedUrl] = useState<string | null>(null);

	const url = src ?? (nationality ? getNationalityFlag(nationality) : null);
	const isAvailable = Boolean(url && url !== failedUrl);

	const classNames = [
		isAvailable ? styles.nationalityFlag : styles.nationalityFlag_placeholder,
		className,
	]
		.filter(Boolean)
		.join(" ");

	if (!isAvailable || !url) {
		return (
			<span
				className={classNames}
				role="img"
				aria-label={
					nationality
						? `Bandera no disponible: ${nationality}`
						: "Nacionalidad no disponible"
				}
				title={nationality ?? "Nacionalidad no disponible"}
			>
				—
			</span>
		);
	}

	return (
		<img
			className={classNames}
			src={url}
			alt={nationality ?? "Bandera de nacionalidad"}
			loading={loading}
			decoding={decoding}
			onError={() => setFailedUrl(url)}
		/>
	);
}
