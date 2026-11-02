import type { usePlayerRankingsPage } from "@/features/players/hooks";
import { Trophy } from "@/shared/icons";
import styles from "./RankingsEmpty.module.scss";

type RankingMode = ReturnType<typeof usePlayerRankingsPage>["mode"];

interface Props {
	mode: RankingMode;
}

export default function RankingsEmpty({ mode }: Props) {
	return (
		<div className={styles.emptyState} role="status">
			<div className={styles.emptyState__iconWrapper}>
				<Trophy
					size={40}
					className={styles.emptyState__icon}
					aria-hidden="true"
				/>
			</div>

			<h2 className={styles.emptyState__title}>
				No hay clasificaciones disponibles
			</h2>

			<p className={styles.emptyState__description}>
				{mode === "PERFORMANCE"
					? "Aún no se han ejecutado corridas de valuación para generar el ranking por rendimiento deportivo."
					: "No existen cotizaciones vigentes en el mercado para listar el ranking por valor."}
			</p>
		</div>
	);
}
