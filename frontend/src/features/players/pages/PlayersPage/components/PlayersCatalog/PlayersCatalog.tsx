import type { Player } from "@/features/players/types";
import { Button } from "@/shared/components";
import PlayerCard from "../PlayerCard";
import styles from "./PlayersCatalog.module.scss";

interface Props {
	isLoading: boolean;
	errorMessage: string | null;
	onRetry: () => void;
	players: Player[];
	hasActiveFilters: boolean;
}

export default function PlayersCatalog({
	isLoading,
	errorMessage,
	onRetry,
	players,
	hasActiveFilters,
}: Props) {
	if (errorMessage) {
		return (
			<section className={styles.playersCatalog__message} role="alert">
				<p>{errorMessage}</p>
				<Button type="button" onClick={onRetry}>
					Reintentar
				</Button>
			</section>
		);
	}

	if (isLoading) {
		return (
			<p className={styles.playersCatalog__message} role="status">
				Cargando jugadores…
			</p>
		);
	}

	if (players.length === 0) {
		return (
			<p className={styles.playersCatalog__message} role="status">
				{hasActiveFilters
					? "No hay jugadores que coincidan con los filtros."
					: "No hay jugadores disponibles."}
			</p>
		);
	}

	return (
		<section className={styles.playersCatalog__grid} aria-label="Jugadores">
			{players.map((player) => (
				<PlayerCard key={player.id} player={player} />
			))}
		</section>
	);
}
