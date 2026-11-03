import { useRotatingIndex } from "@/features/auth/hooks";
import { footballVisuals } from "@/features/auth/utils/footballVisuals";
import styles from "./AuthVisualPanel.module.scss";

const ROTATION_INTERVAL = 8000;

export default function AuthVisualPanel() {
	const currentVisual = useRotatingIndex(footballVisuals, ROTATION_INTERVAL);

	return (
		<aside
			className={styles.authVisualPanel}
			aria-label="El mercado de Football Market"
		>
			<div className={styles.authVisualPanel__backgrounds} aria-hidden="true">
				{footballVisuals.map((visual, index) => (
					<img
						key={visual}
						className={`${styles.authVisualPanel__background} ${
							index === currentVisual
								? styles.authVisualPanel__background_active
								: ""
						}`}
						src={visual}
						alt=""
					/>
				))}
			</div>

			<div className={styles.authVisualPanel__overlay} aria-hidden="true" />

			<div className={styles.authVisualPanel__content}>
				<div>
					<p className={styles.authVisualPanel__eyebrow}>
						EL JUEGO EMPIEZA CONTIGO
					</p>

					<h2 className={styles.authVisualPanel__title}>
						Visión de juego.
						<br />
						Mentalidad de manager.
					</h2>

					<p className={styles.authVisualPanel__description}>
						El fútbol se vive en el campo. Tu próximo equipo empieza con una
						decisión.
					</p>
				</div>

				<div className={styles.authVisualPanel__caption}>
					<span className={styles.authVisualPanel__captionDot} />
					CONSTRUYE TU VISIÓN DEL FÚTBOL
				</div>
			</div>
		</aside>
	);
}
