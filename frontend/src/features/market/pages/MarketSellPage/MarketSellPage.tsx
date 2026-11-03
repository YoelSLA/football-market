import MarketFilters from "@/features/market/components/MarketFilters";
import MarketPageHeading from "@/features/market/components/MarketPageHeading";
import MarketPagination from "@/features/market/components/MarketPagination";
import MarketPlayerSummary from "@/features/market/components/MarketPlayerSummary";
import { useMarketSellPage } from "@/features/market/hooks";
import type { MarketPlayer } from "@/features/market/types";
import { formatCredits } from "@/features/market/utils";
import { Button } from "@/shared/components";
import { Tag } from "@/shared/icons";
import styles from "./MarketSellPage.module.scss";
import SellTokenModal from "./SellTokenModal";

interface PlayerHolding extends MarketPlayer {
	totalQuantity: number;
	availableQuantity: number;
	reservedQuantity: number;
}

interface Props {
	players: PlayerHolding[];
	isLoadingPlayers: boolean;
	isPlayersError: boolean;
	errorMessage?: string | null;
	onRetryPlayers?: () => void;
	onSellSuccess?: (playerId: number) => void;
	onRefreshHoldings?: (playerId: number) => void;
}

export default function MarketSellPage({
	players,
	isLoadingPlayers,
	isPlayersError,
	errorMessage,
	onRetryPlayers,
	onSellSuccess,
	onRefreshHoldings,
}: Props) {
	const sell = useMarketSellPage({ players, onSellSuccess, onRefreshHoldings });
	const totals = players.reduce(
		(accumulator, player) => ({
			total: accumulator.total + player.totalQuantity,
			available: accumulator.available + player.availableQuantity,
			reserved: accumulator.reserved + player.reservedQuantity,
		}),
		{ total: 0, available: 0, reserved: 0 },
	);
	return (
		<div className={styles.sellPage}>
			<header className={styles.sellPage__header}>
				<MarketPageHeading
					title="Vender tokens"
					description="Publicá tus tokens en el mercado secundario."
					icon={Tag}
				/>
				<div className={styles.sellPage__totals}>
					<p>
						Total<strong>{totals.total}</strong>
					</p>
					<p>
						Disponibles<strong>{totals.available}</strong>
					</p>
					<p>
						Reservados<strong>{totals.reserved}</strong>
					</p>
				</div>
			</header>
			<div className={styles.sellPage__content}>
				<MarketFilters
					active
					leagues={sell.leagues}
					positions={sell.positions}
					search={sell.searchInput}
					league={sell.filters.league}
					position={sell.filters.position}
					availability={sell.filters.availability}
					sort={sell.filters.sort}
					onSearchChange={sell.setSearchInput}
					onLeagueChange={(value) => sell.updateFilter("league", value)}
					onPositionChange={(value) => sell.updateFilter("position", value)}
					onAvailabilityChange={(value) =>
						sell.updateFilter("availability", value)
					}
					onSortChange={(value) => sell.updateFilter("sort", value)}
					onApply={sell.applyFilters}
					onClear={sell.clearFilters}
				/>
				<main className={styles.sellPage__main}>
					{isLoadingPlayers && (
						<p role="status" className={styles.sellPage__status}>
							Cargando jugadores y tenencias…
						</p>
					)}
					{!isLoadingPlayers && isPlayersError && (
						<div role="alert" className={styles.sellPage__status}>
							<p>{errorMessage ?? "No se pudo cargar el mercado."}</p>
							{onRetryPlayers && (
								<Button onClick={onRetryPlayers}>Reintentar</Button>
							)}
						</div>
					)}
					{!isLoadingPlayers &&
						!isPlayersError &&
						sell.players.length === 0 && (
							<p className={styles.sellPage__status}>
								No hay jugadores con tenencias para publicar.
							</p>
						)}
					{!isLoadingPlayers && !isPlayersError && sell.players.length > 0 && (
						<div className={styles.sellPage__grid}>
							{sell.players.map((player) => {
								const quote = sell.quoteQuery.getQuoteState(player.id);
								return (
									<article className={styles.sellPage__card} key={player.id}>
										<MarketPlayerSummary player={player} />
										<div className={styles.sellPage__amounts}>
											<span>
												Total<strong>{player.totalQuantity}</strong>
											</span>
											<span>
												Disponibles<strong>{player.availableQuantity}</strong>
											</span>
											<span>
												Reservados<strong>{player.reservedQuantity}</strong>
											</span>
										</div>
										<div className={styles.sellPage__offer}>
											{quote.status === "available" && quote.quote ? (
												<p>
													<span>Precio actual de referencia</span>
													<strong>{formatCredits(quote.quote.price)}</strong>
												</p>
											) : (
												<p className={styles.sellPage__quoteStatus}>
													{quote.status === "missing"
														? "Cotización no disponible"
														: "Cotización pendiente"}
												</p>
											)}
											<Button
												disabled={player.availableQuantity <= 0}
												aria-label={`Vender tokens de ${player.name}`}
												onClick={(event) =>
													sell.open(player, event.currentTarget)
												}
											>
												<Tag aria-hidden="true" /> Publicar venta
											</Button>
										</div>
									</article>
								);
							})}
						</div>
					)}
				</main>
			</div>
			{!isLoadingPlayers && !isPlayersError && sell.totalPlayers > 0 && (
				<div className={styles.sellPage__footer}>
					<MarketPagination
						page={sell.page}
						totalPages={sell.totalPages}
						totalPlayers={sell.totalPlayers}
						visiblePlayers={sell.players.length}
						pageSize={sell.pageSize}
						onPageChange={sell.setPage}
					/>
				</div>
			)}
			<SellTokenModal
				isOpen={sell.isOpen}
				player={sell.selectedPlayer}
				availableQuantity={sell.selectedPlayer?.availableQuantity ?? 0}
				reservedQuantity={sell.selectedPlayer?.reservedQuantity ?? 0}
				quote={sell.selectedQuote}
				quoteStatus={sell.selectedQuoteState?.status ?? "loading"}
				quoteHttpStatus={sell.selectedQuoteState?.httpStatus}
				canRetryQuote={sell.selectedQuoteState?.canRetry ?? false}
				onRetryQuote={sell.selectedQuoteState?.onRetry ?? (() => {})}
				estimation={sell.estimation}
				formRegister={sell.form.register}
				formError={
					sell.form.formState.errors.quantity?.message ??
					(Number(sell.form.watch("quantity")) > sell.form.maximum
						? "Cantidad superior a los tokens disponibles."
						: undefined)
				}
				quantity={sell.form.watch("quantity")}
				maximum={sell.form.maximum}
				presets={sell.form.presets}
				onQuantityChange={sell.form.setQuantity}
				result={sell.result}
				errorMessage={sell.errorMessage}
				confirmationBlockReason={sell.confirmationBlockReason}
				isBusy={sell.isBusy}
				canConfirm={sell.canConfirm}
				onClose={sell.close}
				onConfirm={sell.confirm}
				triggerRef={sell.triggerRef}
				initialFocusRef={sell.initialFocusRef}
			/>
		</div>
	);
}
