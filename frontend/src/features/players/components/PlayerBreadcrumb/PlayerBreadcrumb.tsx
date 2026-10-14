import { Link } from "react-router-dom";
import { DEMO_MESSAGES } from "../../constants";
import styles from "./PlayerBreadcrumb.module.scss";

interface PlayerBreadcrumbProps {
	playerName?: string;
}

/**
 * Breadcrumb del detalle: «Jugadores > nombre real». Sin identidad resuelta no muestra
 * ningún nombre, ni ficticio ni del jugador anterior.
 */
export function PlayerBreadcrumb({ playerName }: PlayerBreadcrumbProps) {
	return (
		<nav
			className={styles["player-breadcrumb"]}
			aria-label="Ruta de navegación"
		>
			<ol>
				<li>
					<Link to="/players">{DEMO_MESSAGES.breadcrumbRoot}</Link>
				</li>
				{playerName ? <li aria-current="page">{playerName}</li> : null}
			</ol>
		</nav>
	);
}
