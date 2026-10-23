import type { Location } from "react-router-dom";
import styles from "./Navbar.module.scss";
import NavbarAccount from "./NavbarAccount";
import NavbarBrand from "./NavbarBrand";
import NavbarSections from "./NavbarSections";
import NavbarTabs from "./NavbarTabs";
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
 * Componen la identidad de marca, la navegación principal,
 * la navegación secundaria (pestañas) y la cuenta de usuario.
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
			<div className={styles.navbar__top}>
				<NavbarBrand />
				<NavbarSections
					sections={sections}
					activeSectionKey={active.sectionKey}
				/>
				<NavbarAccount userName={userName} onLogout={onLogout} />
			</div>
			{activeSection && activeSection.tabs.length > 0 && (
				<NavbarTabs tabs={activeSection.tabs} activeTabKey={active.tabKey} />
			)}
		</header>
	);
}
