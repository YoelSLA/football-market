import styles from "./AuthVisualPanel.module.scss";

export function AuthVisualPanel() {
	return (
		<aside className={styles["auth-visual-panel"]} aria-label="El mercado de Football Market">
			<p className={styles["auth-visual-panel__eyebrow"]}>EL JUEGO EMPIEZA CONTIGO</p>
			<h2 className={styles["auth-visual-panel__title"]}>
				Visión de juego.
				<br />
				Mentalidad de manager.
			</h2>
			<p className={styles["auth-visual-panel__description"]}>
				El fútbol se vive en el campo. Tu próximo equipo empieza con una
				decisión.
			</p>
			<div className={styles["auth-visual-panel__scene"]} aria-hidden="true">
				<div className={styles["auth-visual-panel__pitch"]}>
					<div className={styles["auth-visual-panel__pitch-circle"]} />
				</div>
				<div className={`${styles["auth-visual-panel__player-card"]} ${styles["auth-visual-panel__player-card--back"]}`}>
					<span className={styles["auth-visual-panel__card-number"]}>08</span>
					<div className={`${styles["auth-visual-panel__shirt"]} ${styles["auth-visual-panel__shirt--back"]}`}>FM</div>
					<p className={styles["auth-visual-panel__player-card-caption"]}>CREATIVIDAD</p>
				</div>
				<div className={styles["auth-visual-panel__player-card"]}>
					<div className={styles["auth-visual-panel__card-top"]}>
						<span className={styles["auth-visual-panel__card-number"]}>09</span>
						<span>DEL</span>
					</div>
					<div className={styles["auth-visual-panel__shirt"]}>FM</div>
					<p className={styles["auth-visual-panel__player-card-caption"]}>TU PRÓXIMO FICHAJE</p>
					<div className={styles["auth-visual-panel__stats"]}>
						<span>
							RIT<strong className={styles["auth-visual-panel__stats-value"]}>92</strong>
						</span>
						<span>
							TIR<strong className={styles["auth-visual-panel__stats-value"]}>89</strong>
						</span>
						<span>
							PAS<strong className={styles["auth-visual-panel__stats-value"]}>86</strong>
						</span>
					</div>
				</div>
				<div className={styles["auth-visual-panel__ball"]}>✦</div>
			</div>
			<div className={styles["auth-visual-panel__caption"]}>
				<span className={styles["auth-visual-panel__caption-dot"]} /> CONSTRUYE TU VISIÓN DEL FÚTBOL
			</div>
		</aside>
	);
}
