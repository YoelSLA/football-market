import { useRotatingIndex } from "@/features/auth/hooks";
import { footballVisuals } from "../../assets/footballVisuals";
import styles from "./AuthVisualPanel.module.scss";

const ROTATION_INTERVAL = 8000;

export function AuthVisualPanel() {
  const currentVisual = useRotatingIndex(
    footballVisuals,
    ROTATION_INTERVAL,
  );

  return (
    <aside
      className={styles["auth-visual-panel"]}
      aria-label="El mercado de Football Market"
    >
      <div
        className={styles["auth-visual-panel__backgrounds"]}
        aria-hidden="true"
      >
        {footballVisuals.map((visual, index) => (
          <img
            key={visual}
            className={`${styles["auth-visual-panel__background"]} ${index === currentVisual
              ? styles["auth-visual-panel__background--active"]
              : ""
              }`}
            src={visual}
            alt=""
          />
        ))}
      </div>

      <div
        className={styles["auth-visual-panel__overlay"]}
        aria-hidden="true"
      />

      <div className={styles["auth-visual-panel__content"]}>
        <div>
          <p className={styles["auth-visual-panel__eyebrow"]}>
            EL JUEGO EMPIEZA CONTIGO
          </p>

          <h2 className={styles["auth-visual-panel__title"]}>
            Visión de juego.
            <br />
            Mentalidad de manager.
          </h2>

          <p className={styles["auth-visual-panel__description"]}>
            El fútbol se vive en el campo. Tu próximo equipo empieza con una
            decisión.
          </p>
        </div>

        <div className={styles["auth-visual-panel__caption"]}>
          <span className={styles["auth-visual-panel__caption-dot"]} />
          CONSTRUYE TU VISIÓN DEL FÚTBOL
        </div>
      </div>
    </aside>
  );
}