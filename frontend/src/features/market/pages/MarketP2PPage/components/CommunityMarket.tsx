import { Link } from "react-router-dom";
import MarketCatalogState from "@/features/market/components/MarketCatalogState";
import MarketPlayerCardSkeleton from "@/features/market/components/MarketPlayerCardSkeleton";
import { COMMUNITY_MARKET_PAGE_SIZE } from "@/features/market/constants";
import type {
	CommunityMarketPlayer,
	MarketQuote,
} from "@/features/market/types";
import styles from "./CommunityMarket.module.scss";
import CommunityPlayerCard from "./CommunityPlayerCard";

const SKELETON_ITEMS = Array.from(
	{ length: COMMUNITY_MARKET_PAGE_SIZE },
	(_, index) => index,
);

interface Props {
	isLoading: boolean;
	isError: boolean;
	errorMessage: string | null;
	players: CommunityMarketPlayer[];
	hasFilters: boolean;
	isUpdating: boolean;
	getOfficialQuote?: (playerId: number) => MarketQuote | null;
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
	getOfficialQuote,
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
					<CommunityPlayerCard
						key={player.id}
						player={player}
						officialQuote={getOfficialQuote?.(player.id)}
						onOpenPlayer={onOpenPlayer}
					/>
				))}
			</div>
		);
	};

	return (
		<main className={styles.catalog} aria-busy={isLoading || isUpdating}>
			{isUpdating && !isLoading && !isError && (
				<span className={styles.catalog__status} role="status">
					Actualizando ofertas…
				</span>
			)}

			{renderContent()}
		</main>
	);
}
