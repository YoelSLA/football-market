import { Link } from "react-router-dom";
import NavbarIcon from "@/shared/components/Navbar/NavbarIcon";
import styles from "./NavbarBrand.module.scss";

interface Props {
	/**
	 * URL de navegación al hacer clic en la marca.
	 * Por defecto apunta a la página de inicio.
	 */
	to?: string;
}

/**
 * Identidad visual de Football Market.
 * Comprende el logotipo, el nombre de la aplicación y la navegación hacia inicio.
 */
export default function NavbarBrand({ to = "/home" }: Props) {
	return (
		<Link
			to={to}
			className={styles.navbarBrand}
			aria-label="Football Market - Inicio"
		>
			<NavbarIcon name="brand" />
			<span>Football Market</span>
		</Link>
	);
}
