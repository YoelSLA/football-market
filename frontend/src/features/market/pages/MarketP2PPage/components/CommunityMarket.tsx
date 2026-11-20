import { Link } from "react-router-dom";
import MarketCatalogState from "@/features/market/components/MarketCatalogState";
import MarketPlayerCardSkeleton from "@/features/market/components/MarketPlayerCardSkeleton";
import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import type { CommunityMarketPlayer } from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { Button } from "@/shared/components";
import styles from "./CommunityMarket.module.scss";

const SKELETON_ITEMS = Array.from({ length: 8 }, (_, index) => index);

interface Props {
	isLoading: boolean;
	isError: boolean;
	errorMessage: string | null;
	players: CommunityMarketPlayer[];
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
	hasFilters,
	isUpdating,
	onRetry,
	onOpenPlayer,
}: Props) {
	const renderContent = () => {
		if (isError)
			return (
				<MarketCatalogState
					variant="error"
					message={errorMessage ?? "No pudimos cargar las ofertas de usuarios."}
					onRetry={onRetry}
				/>
			);

		if (isLoading || isUpdating)
			return (
				<div className={styles.catalog__grid}>
					{SKELETON_ITEMS.map((item) => (
						<MarketPlayerCardSkeleton key={item} variant="community" />
					))}
				</div>
			);

		if (players.length === 0)
			return (
				<MarketCatalogState
					variant="empty"
					message={
						hasFilters
							? "No hay resultados para los filtros seleccionados."
							: "No hay publicaciones P2P disponibles."
					}
				>
					{!hasFilters && <Link to="/market/buy">Ir al Mercado Oficial</Link>}
				</MarketCatalogState>
			);

		return (
			<div className={styles.catalog__grid}>
				{players.map((player) => (
					<article className={styles.card} key={player.id}>
						<div className={styles.card__identity}>
							<MarketPlayerSummary player={player} variant="communityCard" />
						</div>
						<div className={styles.card__metrics}>
							<span>Publicaciones</span>
							<span>Tokens ofrecidos</span>
							<span>Mejor precio</span>
							<strong>{player.listingCount}</strong>
							<strong>{player.availableQuantity}</strong>
							<strong>{formatCredits(player.minimumUnitPrice)}</strong>
							<span className={styles.card__metricCaption}>
								Ofertas activas
							</span>
							<span className={styles.card__metricCaption}>Disponibles</span>
							<span className={styles.card__metricCaption}>Por token</span>
						</div>
						<div className={styles.card__actions}>
							<Button
								className={styles.card__action}
								onClick={() => onOpenPlayer(player)}
							>
								Ver ofertas
							</Button>
						</div>
					</article>
				))}
			</div>
		);
	};
	return (
		<section
			className={styles.catalog}
			aria-label="Jugadores con ofertas"
			aria-busy={isLoading || isUpdating ? "true" : undefined}
		>
			{isUpdating && (
				<span className={styles.catalog__status} role="status">
					Actualizando ofertas…
				</span>
			)}
			{renderContent()}
		</section>
	);
}
