import { useState } from "react";
import { getTeamShield } from "@/shared/utils/teamShield";
import styles from "./TeamBadge.module.scss";

export interface TeamBadgeProps {
	teamName: string;
	leagueName?: string;
	src?: string | null;
	className?: string;
	loading?: "lazy" | "eager";
	decoding?: "async" | "auto" | "sync";
}

export default function TeamBadge({
	teamName,
	leagueName,
	src,
	className,
	loading = "lazy",
	decoding = "async",
}: TeamBadgeProps) {
	const [failedUrl, setFailedUrl] = useState<string | null>(null);

	const url = src ?? (leagueName ? getTeamShield(leagueName, teamName) : null);
	const isAvailable = Boolean(url && url !== failedUrl);

	const classNames = [
		isAvailable ? styles.teamBadge : styles.teamBadge_placeholder,
		className,
	]
		.filter(Boolean)
		.join(" ");

	if (!isAvailable || !url) {
		return (
			<span
				className={classNames}
				role="img"
				aria-label={`Escudo no disponible: ${teamName}`}
				title={`Escudo no disponible: ${teamName}`}
			>
				◇
			</span>
		);
	}

	return (
		<img
			className={classNames}
			src={url}
			alt={`Escudo de ${teamName}`}
			loading={loading}
			decoding={decoding}
			onError={() => setFailedUrl(url)}
		/>
	);
}
