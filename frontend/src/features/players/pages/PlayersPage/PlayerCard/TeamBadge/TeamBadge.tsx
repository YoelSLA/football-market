import { useState } from "react";
import { getTeamLogo } from "@/features/players/utils";
import styles from "./TeamBadge.module.scss";

interface Props {
	teamName: string;
}

export default function TeamBadge({ teamName }: Props) {
	const url = getTeamLogo(teamName);
	const [failed, setFailed] = useState(false);
	return url && !failed ? (
		<img
			className={styles.teamBadge}
			src={url}
			alt={`Escudo de ${teamName}`}
			loading="lazy"
			decoding="async"
			onError={() => setFailed(true)}
		/>
	) : (
		<span
			className={styles.teamBadge_placeholder}
			role="img"
			aria-label={`Escudo no disponible: ${teamName}`}
			title="Escudo no disponible"
		>
			◇
		</span>
	);
}
