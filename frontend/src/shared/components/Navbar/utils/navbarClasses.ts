import styles from "../Navbar.module.scss";

/** Estados visuales de los accesos principales. */
export function sectionClasses(active: boolean): string {
	return [
		styles.navbar__section,
		active ? styles.navbar__section_active : "",
	]
		.filter(Boolean)
		.join(" ");
}

export function disabledSectionClasses(): string {
	return `${styles.navbar__section} ${styles.navbar__section_disabled}`;
}
