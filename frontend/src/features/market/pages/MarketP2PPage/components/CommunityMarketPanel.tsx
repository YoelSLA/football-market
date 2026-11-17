import MarketPagination from "@/features/market/components/MarketPagination";
import type { CommunityMarketState } from "@/features/market/hooks";
import CommunityListingsPanel from "./CommunityListingsPanel";
import CommunityMarket from "./CommunityMarket";
import CommunityMarketFilters from "./CommunityMarketFilters";
import styles from "./CommunityMarketPanel.module.scss";

interface Props {
	community: CommunityMarketState;
}

export default function CommunityMarketPanel({ community }: Props) {
	if (community.selectedPlayerId !== null) {
		return (
			<div className={styles.communityPanel__content}>
				{community.isLoading ? (
					<p role="status">Cargando publicaciones…</p>
				) : community.isError ? (
					<div role="alert">
						<p>{community.errorMessage}</p>
						<button type="button" onClick={community.retry}>
							Reintentar
						</button>
					</div>
				) : community.selectedPlayer ? (
					<CommunityListingsPanel
						player={community.selectedPlayer}
						onBack={community.closePlayer}
						onBuy={community.openListing}
					/>
				) : (
					<section role="status" aria-labelledby="community-player-empty">
						<h2 id="community-player-empty">Jugador sin ofertas disponibles</h2>
						<p>
							Las publicaciones pudieron vencer o dejar de estar disponibles.
						</p>
						<button type="button" onClick={community.closePlayer}>
							Volver al catálogo
						</button>
					</section>
				)}
			</div>
		);
	}

	return (
		<>
			<div className={styles.communityPanel__content}>
				<CommunityMarketFilters
					filters={community.filters}
					leagues={community.leagues}
					positions={community.positions}
					onChange={community.updateFilters}
					onClear={community.clearFilters}
				/>
				<CommunityMarket
					isLoading={community.isLoading}
					isError={community.isError}
					errorMessage={community.errorMessage}
					players={community.players}
					totalPlayers={community.totalPlayers}
					hasFilters={
						community.filters.search !== "" ||
						community.filters.league !== "all" ||
						community.filters.position !== "all"
					}
					isUpdating={community.isUpdating}
					onRetry={community.retry}
					onOpenPlayer={community.openPlayer}
				/>
			</div>
			{community.totalPages > 1 && (
				<div className={styles.communityPanel__footer}>
					<MarketPagination
						page={community.page}
						totalPages={community.totalPages}
						totalPlayers={community.totalPlayers}
						visiblePlayers={community.players.length}
						pageSize={12}
						showSummary={false}
						onPageChange={community.setPage}
					/>
				</div>
			)}
		</>
	);
}
