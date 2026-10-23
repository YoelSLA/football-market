import { useState } from "react";
import { getNationalityFlag } from "@/features/players/utils";
import styles from "./NationalityFlag.module.scss";

interface Props {
	nationality: string | null;
}

export default function NationalityFlag({ nationality }: Props) {
	const url = getNationalityFlag(nationality);
	const [failed, setFailed] = useState(false);
	return url && !failed ? (
		<img
			className={styles.nationalityFlag}
			src={url}
			alt={nationality ?? ""}
			loading="lazy"
			onError={() => setFailed(true)}
		/>
	) : (
		<span
			className={styles.nationalityFlag_placeholder}
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
