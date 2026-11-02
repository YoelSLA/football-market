import Button from "@/shared/components/Button";
import { CircleAlert, RotateCw } from "@/shared/icons";
import styles from "./RankingsError.module.scss";

interface Props {
	error: unknown;
	onRetry: () => void;
}

export default function RankingsError({ error, onRetry }: Props) {
	return (
		<section className={styles.errorAlert} role="alert">
			<CircleAlert
				size={20}
				className={styles.errorAlert__icon}
				aria-hidden="true"
			/>

			<div className={styles.errorAlert__content}>
				<strong className={styles.errorAlert__title}>
					Error al cargar el ranking
				</strong>

				<p className={styles.errorAlert__message}>
					{error instanceof Error
						? error.message
						: "No se pudieron obtener las clasificaciones vigentes. Por favor intenta nuevamente."}
				</p>
			</div>

			<Button onClick={onRetry} className={styles.errorAlert__retryButton}>
				<RotateCw size={14} aria-hidden="true" />
				<span>Reintentar</span>
			</Button>
		</section>
	);
}
