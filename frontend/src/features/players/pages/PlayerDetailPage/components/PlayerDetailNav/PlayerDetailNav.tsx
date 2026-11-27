import { Link, useLocation } from "react-router-dom";
import { validateReturnRoute } from "@/features/players/utils";
import { ArrowLeft } from "@/shared/icons";
import styles from "./PlayerDetailNav.module.scss";

interface Props {
	playerName?: string;
	leagueName?: string;
}

export default function PlayerDetailNav({ playerName, leagueName }: Props) {
	const location = useLocation();
	const fromState = (location.state as { from?: unknown } | null)?.from;
	const { path: returnUrl, label } = validateReturnRoute(fromState);
	const compactLabel = label
		.replace(/^Volver al?\s+/i, "")
		.replace(/^./, (c) => c.toUpperCase());

	return (
		<nav className={styles.playerDetailNav} aria-label="Navegación secundaria">
			<div className={styles.playerDetailNav__breadcrumb}>
				<Link
					to={returnUrl}
					className={styles.playerDetailNav__backLink}
					aria-label={label}
				>
					<ArrowLeft size={14} aria-hidden="true" />
					<span>{compactLabel || "Volver a Jugadores"}</span>
				</Link>
				{leagueName && (
					<>
						<span className={styles.playerDetailNav__sep}>/</span>
						<span className={styles.playerDetailNav__crumb}>{leagueName}</span>
					</>
				)}
				{playerName && (
					<>
						<span className={styles.playerDetailNav__sep}>/</span>
						<span className={styles.playerDetailNav__current}>
							{playerName}
						</span>
					</>
				)}
			</div>
		</nav>
	);
}
