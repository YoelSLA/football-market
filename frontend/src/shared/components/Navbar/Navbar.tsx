import type { Location } from "react-router-dom";
import styles from "./Navbar.module.scss";
import NavbarTabs from "./NavbarTabs";
import NavbarTop from "./NavbarTop";
import type { NavigationResolver, NavSection } from "./types";

interface Props {
	sections: NavSection[];
	location: Pick<Location, "pathname">;
	resolveActiveNavigation: NavigationResolver;
	userName: string | null;
	onLogout: () => void;
}

/**
 * Barra de navegación global de Football Market.
 *
 * Compone la franja superior (NavbarTop: marca, navegación principal y cuenta)
 * y la navegación secundaria (NavbarTabs: pestañas de sección).
 * Utiliza una única identidad visual (estilo financiero) en toda la aplicación.
 */
export default function Navbar({
	sections,
	location,
	resolveActiveNavigation,
	userName,
	onLogout,
}: Props) {
	const active = resolveActiveNavigation(sections, location.pathname);
	const activeSection = sections.find(
		(section) => section.key === active.sectionKey,
	);

	return (
		<header className={styles.navbar}>
			<NavbarTop
				sections={sections}
				activeSectionKey={active.sectionKey}
				userName={userName}
				onLogout={onLogout}
			/>
			{activeSection && activeSection.tabs.length > 0 && (
				<NavbarTabs tabs={activeSection.tabs} activeTabKey={active.tabKey} />
			)}
		</header>
	);
}
