import type { OfficialMarketPlayer } from "@/features/market/types";
import MarketPlayerCard from "../MarketPlayerCard";
import MarketCatalogState from "./components/MarketCatalogState";
import styles from "./MarketBuyCatalog.module.scss";

interface Props {
	isLoading: boolean;
	isUpdating: boolean;
	isError: boolean;
	errorMessage?: string | null;
	onRetry?: () => void;
	players: OfficialMarketPlayer[];
	hasFilters: boolean;
	onBuyClick: (
		player: OfficialMarketPlayer,
		triggerEl: HTMLElement | null,
	) => void;
}

export default function MarketBuyCatalog({
	isLoading,
	isUpdating,
	isError,
	errorMessage,
	onRetry,
	players,
	hasFilters,
	onBuyClick,
}: Props) {
	const renderContent = () => {
		if (isLoading) {
			return (
				<MarketCatalogState
					variant="loading"
					message="Cargando Mercado Oficial…"
				/>
			);
		}

		if (isError) {
			return (
				<MarketCatalogState
					variant="error"
					message={errorMessage || "No se pudo cargar el Mercado Oficial."}
					onRetry={onRetry}
				/>
			);
		}

		if (players.length === 0) {
			return (
				<MarketCatalogState
					variant="empty"
					message={
						hasFilters
							? "No hay resultados para los filtros seleccionados."
							: "El Mercado Oficial no tiene jugadores disponibles."
					}
				/>
			);
		}

		return (
			<div className={styles.buyCatalog__grid}>
				{players.map((player) => (
					<MarketPlayerCard
						key={player.id}
						player={player}
						onBuyClick={onBuyClick}
					/>
				))}
			</div>
		);
	};

	return (
		<main className={styles.buyCatalog} aria-busy={isLoading || isUpdating}>
			{isUpdating && !isLoading && !isError && (
				<span className={styles.buyCatalog__updating} role="status">
					Actualizando resultados…
				</span>
			)}

			{renderContent()}
		</main>
	);
}
