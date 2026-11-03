import { LoaderCircle } from "@/shared/icons";
import styles from "./RankingsLoading.module.scss";

export default function RankingsLoading() {
	return (
		<div className={styles.loadingState} role="status" aria-live="polite">
			<LoaderCircle
				size={36}
				className={styles.loadingSpinner}
				aria-hidden="true"
			/>

			<p className={styles.loadingState__text}>
				Cargando clasificación de jugadores…
			</p>
		</div>
	);
}
