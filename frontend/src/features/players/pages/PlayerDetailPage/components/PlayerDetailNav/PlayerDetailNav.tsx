import { Link, useLocation } from "react-router-dom";
import { validateReturnRoute } from "@/features/players/utils";
import { ArrowLeft } from "@/shared/icons";
import styles from "./PlayerDetailNav.module.scss";

export default function PlayerDetailNav() {
	const location = useLocation();
	const fromState = (location.state as { from?: unknown } | null)?.from;
	const { path: returnUrl, label } = validateReturnRoute(fromState);
	const compactLabel = label
		.replace(/^Volver al?\s+/i, "")
		.replace(/^./, (c) => c.toUpperCase());

	return (
		<nav className={styles.playerDetailNav} aria-label="Navegación secundaria">
			<Link
				to={returnUrl}
				className={styles.playerDetailNav__backLink}
				aria-label={label}
			>
				<ArrowLeft size={14} aria-hidden="true" />
				<span>{compactLabel || "Jugadores"}</span>
			</Link>
		</nav>
	);
}
