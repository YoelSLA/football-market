import { NavLink } from "react-router-dom";
import SoonBadge from "@/shared/components/Navbar/ComingSoonBadge";
import NavbarIcon from "@/shared/components/Navbar/NavbarIcon";
import type { NavSection } from "@/shared/components/Navbar/types";
import styles from "./NavbarNavigation.module.scss";
import { disabledSectionClasses, sectionClasses } from "./navbarClasses";

interface Props {
	sections: NavSection[];
	activeSectionKey: string | null;
}

/**
 * Navegación principal de la aplicación.
 * Renderiza las secciones principales con sus estados activos y deshabilitados.
 */
export default function NavbarNavigation({
	sections,
	activeSectionKey,
}: Props) {
	return (
		<nav className={styles.navbarSections} aria-label="Secciones principales">
			<ul className={styles.navbarSectionList}>
				{sections.map((section) => (
					<li key={section.key} className={styles.navbarSectionItem}>
						{section.path ? (
							<NavLink
								to={section.path}
								className={sectionClasses(section.key === activeSectionKey)}
								aria-current={
									section.key === activeSectionKey ? "page" : undefined
								}
							>
								<NavbarIcon name={section.icon} />
								<span>{section.label}</span>
							</NavLink>
						) : (
							<span className={disabledSectionClasses()} aria-disabled="true">
								<NavbarIcon name={section.icon} />
								<span>{section.label}</span>
								<SoonBadge />
							</span>
						)}
					</li>
				))}
			</ul>
		</nav>
	);
}
