import MarketFilters from "@/features/market/components/MarketFilters";
import MarketPagination from "@/features/market/components/MarketPagination";
import type { CommunityMarketState } from "@/features/market/hooks";
import { Button } from "@/shared/components";
import CommunityListingsPanel from "./CommunityListingsPanel";
import CommunityMarket from "./CommunityMarket";
import styles from "./CommunityMarketPanel.module.scss";

interface Props {
	community: CommunityMarketState;
}

interface DetailEmptyStateProps {
	id: string;
	title: string;
	description: string;
	action: string;
	onAction: () => void;
}

function DetailEmptyState({
	id,
	title,
	description,
	action,
	onAction,
}: DetailEmptyStateProps) {
	return (
		<section
			className={styles.communityPanel__empty}
			role="status"
			aria-labelledby={id}
		>
			<div className={styles.communityPanel__emptyContent}>
				<p className={styles.communityPanel__emptyEyebrow}>Mercado P2P</p>
				<h2 id={id}>{title}</h2>
				<p>{description}</p>
			</div>
			<Button type="button" onClick={onAction}>
				{action}
			</Button>
		</section>
	);
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
						hasPriceLimit={community.filters.maxPrice !== null}
						officialQuote={community.getOfficialQuote(
							community.selectedPlayer.id,
						)}
						onBack={community.closePlayer}
						onClearPriceLimit={community.clearMaxPrice}
						onBuy={community.openListing}
					/>
				) : community.unfilteredSelectedPlayer &&
					community.filters.maxPrice !== null ? (
					<DetailEmptyState
						id="community-price-empty"
						title="No hay ofertas dentro del precio máximo"
						description={`${community.unfilteredSelectedPlayer.name} tiene publicaciones fuera del límite seleccionado.`}
						action="Ver todas las publicaciones"
						onAction={community.clearMaxPrice}
					/>
				) : (
					<DetailEmptyState
						id="community-player-empty"
						title="Jugador sin ofertas disponibles"
						description="Las publicaciones pudieron venderse, vencer o dejar de estar disponibles. Volvé al catálogo para explorar otras opciones."
						action="Volver al catálogo"
						onAction={community.closePlayer}
					/>
				)}
			</div>
		);
	}

	return (
		<>
			<div className={styles.communityPanel__content}>
				<MarketFilters
					variant="community"
					filters={community.filters}
					leagues={community.leagues}
					teams={community.teams}
					positions={community.positions}
					priceRange={community.priceRange}
					onChange={community.updateFilters}
					onMaxPriceCommit={community.commitMaxPrice}
					onClear={community.clearFilters}
					isUpdating={community.isUpdating}
					summary={`${community.totalPlayers.toLocaleString("es-AR")} resultados`}
				/>
				<CommunityMarket
					isLoading={community.isLoading}
					isError={community.isError}
					errorMessage={community.errorMessage}
					players={community.players}
					hasFilters={
						community.filters.search !== "" ||
						community.filters.leagueId !== null ||
						community.filters.teamId !== null ||
						community.filters.position !== null ||
						community.filters.maxPrice !== null
					}
					isUpdating={community.isUpdating}
					getOfficialQuote={community.getOfficialQuote}
					onRetry={community.retry}
					onOpenPlayer={community.openPlayer}
				/>
			</div>
			{!community.isLoading && !community.isError && (
				<div className={styles.communityPanel__footer}>
					<MarketPagination
						page={community.page}
						totalPages={community.totalPages}
						totalPlayers={community.totalPlayers}
						visiblePlayers={community.players.length}
						pageSize={8}
						showSummary={false}
						variant="official"
						onPageChange={community.setPage}
					/>
				</div>
			)}
		</>
	);
}
