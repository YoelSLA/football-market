import { NavLink } from "react-router-dom";
import type { NavSection } from "@/shared/components/Navbar/types";
import NavIcon from "../NavIcon";
import SoonBadge from "../SoonBadge";
import { disabledSectionClasses, sectionClasses } from "../utils";

import styles from "./NavbarSections.module.scss";

interface Props {
	sections: NavSection[];
	activeSectionKey: string | null;
}

/**
 * Navegación principal de la aplicación.
 * Renderiza las secciones principales con sus estados activos y deshabilitados.
 */
export default function NavbarSections({ sections, activeSectionKey }: Props) {
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
								<NavIcon name={section.icon} />
								<span>{section.label}</span>
							</NavLink>
						) : (
							<span className={disabledSectionClasses()} aria-disabled="true">
								<NavIcon name={section.icon} />
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
