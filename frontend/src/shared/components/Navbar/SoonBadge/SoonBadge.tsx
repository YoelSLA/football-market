import styles from "./SoonBadge.module.scss";

/** Comunica el estado futuro sin depender exclusivamente del color. */
export default function SoonBadge() {
	return <span className={styles.soonBadge}>Próximamente</span>;
}
