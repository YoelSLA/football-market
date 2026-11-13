import { Link } from "react-router-dom";
import type { PlayerDetailState } from "@/features/players/types";
import { Button } from "@/shared/components";
import styles from "./PlayerDetailStatus.module.scss";

interface Props {
	state: PlayerDetailState;
	errorMessage?: string | null;
	onRetry: () => void;
}

export default function PlayerDetailStatus({
	state,
	errorMessage,
	onRetry,
}: Props) {
	return (
		<section
			className={styles.detailStatus}
			role={state === "loading" ? "status" : "alert"}
		>
			<p>
				{state === "loading"
					? "Cargando jugador…"
					: state === "invalid-id"
						? "El identificador del jugador no es válido."
						: state === "not-found"
							? "Jugador no encontrado"
							: errorMessage}
			</p>
			{state === "error" && (
				<Button type="button" onClick={onRetry}>
					Reintentar
				</Button>
			)}
			<Link to="/players">Volver a jugadores</Link>
		</section>
	);
}
