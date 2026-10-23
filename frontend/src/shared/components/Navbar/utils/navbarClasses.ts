import styles from "../NavbarSections/NavbarSections.module.scss";

/** Estados visuales de los accesos principales. */
export function sectionClasses(active: boolean): string {
	return [styles.navbarSection, active ? styles.navbarSection_active : ""]
		.filter(Boolean)
		.join(" ");
}

export function disabledSectionClasses(): string {
	return `${styles.navbarSection} ${styles.navbarSection_disabled}`;
}
