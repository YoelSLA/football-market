import type { NavTab } from "@/shared/components/Navbar/types";
import styles from "./NavbarTabs.module.scss";
import TabLink from "./TabLink";

interface Props {
	tabs: NavTab[];
	activeTabKey: string | null;
}

/**
 * Navegación secundaria (pestañas) dentro de la sección activa.
 * Renderiza las pestañas con su estado activo y deshabilitado.
 */
export default function NavbarTabs({ tabs, activeTabKey }: Props) {
	return (
		<nav className={styles.navbarTabs} aria-label="Opciones de la sección">
			<ul className={styles.navbarTabList}>
				{tabs.map((tab) => (
					<li key={tab.key} className={styles.navbarTabItem}>
						<TabLink tab={tab} active={tab.key === activeTabKey} />
					</li>
				))}
			</ul>
		</nav>
	);
}
