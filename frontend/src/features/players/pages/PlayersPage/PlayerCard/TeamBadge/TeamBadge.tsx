import { useState } from "react";
import { getTeamShield } from "@/shared/utils/teamShield";
import styles from "./TeamBadge.module.scss";

interface Props {
	teamName: string;
	leagueName?: string;
}

export default function TeamBadge({ teamName, leagueName }: Props) {
	const url = leagueName ? getTeamShield(leagueName, teamName) : null;
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