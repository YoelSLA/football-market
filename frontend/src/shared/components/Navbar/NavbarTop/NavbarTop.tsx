import type { NavSection } from "@/shared/components/Navbar/types";
import NavbarBrand from "./NavbarBrand";
import NavbarNavigation from "./NavbarNavigation";
import styles from "./NavbarTop.module.scss";
import NavbarAccount from "./NavbarUserActions";

interface Props {
	sections: NavSection[];
	activeSectionKey: string | null;
	userName: string | null;
	onLogout: () => void;
}

/**
 * Franja superior de la barra de navegación global (60px).
 *
 * Contiene la identidad de marca, los enlaces de secciones principales
 * y las acciones de cuenta de usuario.
 */
export default function NavbarTop({
	sections,
	activeSectionKey,
	userName,
	onLogout,
}: Props) {
	return (
		<div className={styles.navbarTop}>
			<NavbarBrand />
			<NavbarNavigation
				sections={sections}
				activeSectionKey={activeSectionKey}
			/>
			<NavbarAccount userName={userName} onLogout={onLogout} />
		</div>
	);
}
