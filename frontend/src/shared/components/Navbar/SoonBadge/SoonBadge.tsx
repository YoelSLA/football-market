import styles from "./SoonBadge.module.scss";

/** Comunica el estado futuro sin depender exclusivamente del color. */
export function SoonBadge() {
	return <span className={styles["soon-badge"]}>Próximamente</span>;
}
