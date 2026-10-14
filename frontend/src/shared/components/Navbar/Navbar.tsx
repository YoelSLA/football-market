import { useId, useState } from "react";
import { Link, NavLink } from "react-router-dom";
import { Button } from "../Button";
import styles from "./Navbar.module.scss";
import { NAV_ITEMS } from "./navItems";

interface Props {
	userName: string | null;
	onLogout: () => void;
}

export default function Navbar({ userName, onLogout }: Props) {
	const [menuOpen, setMenuOpen] = useState(false);
	const menuId = useId();

	const userInitial = userName?.trim().charAt(0).toUpperCase() || "U";

	return (
		<header className={styles.appHeader}>
			{/* LEFT: Logo */}
			<div className={styles.appHeader__left}>
				<Link to="/home" className={styles.appHeader__logo}>
					Football Market
				</Link>
			</div>

			{/* CENTER: Navegación */}
			<nav
				id={menuId}
				className={[
					styles.appHeader__nav,
					menuOpen && styles.appHeader__navOpen,
				]
					.filter(Boolean)
					.join(" ")}
				aria-label="Navegación principal"
			>
				{NAV_ITEMS.map(({ key, to, label }) => (
					<NavLink
						key={key}
						to={to}
						end={key === "home"}
						className={({ isActive }) =>
							[styles.appHeader__link, isActive && styles.appHeader__linkActive]
								.filter(Boolean)
								.join(" ")
						}
						onClick={() => setMenuOpen(false)}
					>
						{label}
					</NavLink>
				))}

				<Button className={styles.appHeader__mobileLogout} onClick={onLogout}>
					Cerrar sesión
				</Button>
			</nav>

			{/* RIGHT: Usuario y acciones */}
			<div className={styles.appHeader__right}>
				<div className={styles.appHeader__user}>
					<div className={styles.appHeader__avatar} aria-hidden="true">
						{userInitial}
					</div>

					<span className={styles.appHeader__userName}>
						{userName || "Usuario"}
					</span>
				</div>

				<Button className={styles.appHeader__logout} onClick={onLogout}>
					Cerrar sesión
				</Button>

				<Button
					className={styles.appHeader__toggle}
					aria-label={menuOpen ? "Cerrar menú" : "Abrir menú"}
					aria-expanded={menuOpen}
					aria-controls={menuId}
					onClick={() => setMenuOpen((value) => !value)}
				>
					{menuOpen ? "✕" : "☰"}
				</Button>
			</div>
		</header>
	);
}
