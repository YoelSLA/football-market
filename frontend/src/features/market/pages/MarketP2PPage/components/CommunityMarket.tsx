import { Link } from "react-router-dom";
import type { CommunityMarketPlayer } from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { Button } from "@/shared/components";
import styles from "./CommunityMarket.module.scss";

interface Props {
	isLoading: boolean;
	isError: boolean;
	errorMessage: string | null;
	players: CommunityMarketPlayer[];
	totalPlayers: number;
	hasFilters: boolean;
	isUpdating: boolean;
	onRetry: () => void;
	onOpenPlayer: (player: CommunityMarketPlayer) => void;
}

export default function CommunityMarket({
	isLoading,
	isError,
	errorMessage,
	players,
	totalPlayers,
	hasFilters,
	isUpdating,
	onRetry,
	onOpenPlayer,
}: Props) {
	if (isLoading)
		return (
			<p className={styles.state} role="status">
				Cargando ofertas de usuarios…
			</p>
		);
	if (isError)
		return (
			<div className={styles.state} role="alert">
				<p>{errorMessage}</p>
				<Button onClick={onRetry}>Reintentar</Button>
			</div>
		);
	if (players.length === 0)
		return (
			<div className={styles.state} role="status">
				<p>
					{hasFilters
						? "No hay resultados para los filtros seleccionados."
						: "No hay publicaciones P2P disponibles."}
				</p>
				{!hasFilters && <Link to="/market/buy">Ir al Mercado Oficial</Link>}
			</div>
		);
	return (
		<section
			className={styles.catalog}
			aria-labelledby="community-market-title"
			aria-busy={isUpdating ? "true" : undefined}
		>
			<header className={styles.catalog__header}>
				<h2 id="community-market-title">Jugadores con ofertas</h2>
				<span>{totalPlayers.toLocaleString("es-AR")} resultados</span>
				{isUpdating && <span role="status">Actualizando ofertas…</span>}
			</header>
			<div className={styles.catalog__grid}>
				{players.map((player) => (
					<article className={styles.card} key={player.id}>
						<h3>{player.name}</h3>
						<p className={styles.card__identity}>
							{player.teamName} · {player.position}
						</p>
						<dl className={styles.card__metrics}>
							<div>
								<dt>Publicaciones</dt>
								<dd>{player.listingCount}</dd>
							</div>
							<div>
								<dt>Tokens ofrecidos</dt>
								<dd>{player.availableQuantity}</dd>
							</div>
							<div>
								<dt>Mejor precio</dt>
								<dd>{formatCredits(player.minimumUnitPrice)} por token</dd>
							</div>
						</dl>
						<Button
							className={styles.card__action}
							onClick={() => onOpenPlayer(player)}
						>
							Ver ofertas
						</Button>
					</article>
				))}
			</div>
		</section>
	);
}
