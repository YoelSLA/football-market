import Button from "@/shared/components/Button";

import styles from "./NavbarAccount.module.scss";

interface Props {
	userName: string | null;
	onLogout: () => void;
}

/**
 * Área de cuenta del usuario en la navegación.
 * Muestra avatar, nombre de usuario y botón de cierre de sesión.
 */
export default function NavbarAccount({ userName, onLogout }: Props) {
	const userInitial = userName?.trim().charAt(0).toUpperCase() || "U";

	return (
		<div className={styles.navbarAccount}>
			<span className={styles.navbarAvatar} aria-hidden="true">
				{userInitial}
			</span>
			<span className={styles.navbarUserName} title={userName ?? undefined}>
				{userName}
			</span>
			<Button className={styles.navbarLogout} onClick={onLogout}>
				Cerrar sesión
			</Button>
		</div>
	);
}
