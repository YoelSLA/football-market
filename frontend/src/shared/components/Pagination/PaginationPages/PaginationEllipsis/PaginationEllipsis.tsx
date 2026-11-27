import styles from "./PaginationEllipsis.module.scss";

export default function PaginationEllipsis() {
	return (
		<span className={styles.ellipsis} aria-hidden="true">
			…
		</span>
	);
}
