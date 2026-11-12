import { useState } from "react";
import { Globe } from "@/shared/icons";
import { getLeagueShield } from "@/shared/utils/leagueShield";
import styles from "./CompetitionLogo.module.scss";

export interface CompetitionLogoProps {
	competitionName: string;
	src?: string | null;
	className?: string;
	decorative?: boolean;
	loading?: "lazy" | "eager";
	decoding?: "async" | "auto" | "sync";
}

export default function CompetitionLogo({
	competitionName,
	src,
	className,
	decorative = false,
	loading = "lazy",
	decoding = "async",
}: CompetitionLogoProps) {
	const [failedUrl, setFailedUrl] = useState<string | null>(null);
	const url = src ?? getLeagueShield(competitionName);
	const isAvailable = Boolean(url && url !== failedUrl);
	const classNames = [styles.competitionLogo, className]
		.filter(Boolean)
		.join(" ");

	if (isAvailable && url) {
		return (
			<span className={classNames} aria-hidden={decorative || undefined}>
				<img
					src={url}
					alt={decorative ? "" : `Logotipo de ${competitionName}`}
					loading={loading}
					decoding={decoding}
					onError={() => setFailedUrl(url)}
				/>
			</span>
		);
	}

	if (decorative) {
		return (
			<span className={classNames} aria-hidden="true">
				<Globe aria-hidden="true" />
			</span>
		);
	}

	return (
		<span
			className={classNames}
			role="img"
			aria-label={`Logotipo no disponible: ${competitionName}`}
			title={competitionName}
		>
			<Globe aria-hidden="true" />
		</span>
	);
}
