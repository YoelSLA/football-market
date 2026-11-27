import { useMemo } from "react";
import { PageLayout } from "@/app/layouts";
import { useMarketTrendsQuery } from "@/features/market/hooks";
import { useMarketQuotesQuery } from "@/features/market/hooks/queries";
import BuyTokenModal from "@/features/market/pages/MarketBuyPage/components/BuyTokenModal";
import { useAllPlayersQuery } from "@/features/players";
import type { Player } from "@/features/players/types";
import { AuthStatus, useAuthStore } from "@/infrastructure/storage";
import { Button } from "@/shared/components";
import { TrendingUp, TriangleAlert } from "@/shared/icons";
import MarketHotAssets from "./components/MarketHotAssets";
import MarketLeagueDemandChart from "./components/MarketLeagueDemandChart";
import MarketMacroMetrics from "./components/MarketMacroMetrics";
import MarketPositionalDemandChart from "./components/MarketPositionalDemandChart";
import MarketSentimentGauge from "./components/MarketSentimentGauge";
import MarketTopValuedPlayers from "./components/MarketTopValuedPlayers";
import MarketTrendsEmptyState from "./components/MarketTrendsEmptyState";
import MarketTrendsSkeleton from "./components/MarketTrendsSkeleton";
import { useMarketTrendsBuyModal } from "./hooks";
import styles from "./MarketTrendsPage.module.scss";

/**
 * Tablero de Tendencias y Sentimiento del Mercado (SPEC-029).
 * - Fila 1: Cockpit Macro 24h (4 KPIs: Volumen, Tokens, Transacciones, Ticket Promedio).
 * - Fila 2: Análisis Triple (Sentimiento Colectivo, Demanda Táctica, Flujo por Liga).
 * - Fila 3: Tablas de Mercado (Activos en Tendencia Top 5 + Mayores Cotizaciones Top 5).
 */
export default function MarketTrendsPage() {
	const { data, isLoading, isError, refetch } = useMarketTrendsQuery();
	const { data: playersData } = useAllPlayersQuery();

	const authenticated = useAuthStore(
		(state) =>
			state.status === AuthStatus.AUTHENTICATED && state.session !== null,
	);

	const quotePlayerIds = useMemo(() => {
		const ids: number[] = [];
		if (data?.topHotPlayers) {
			for (const p of data.topHotPlayers) {
				ids.push(p.playerId);
			}
		}
		return ids;
	}, [data?.topHotPlayers]);

	const quotesQuery = useMarketQuotesQuery(quotePlayerIds, authenticated);
	const buyModal = useMarketTrendsBuyModal();

	const playerMap = useMemo(() => {
		const map = new Map<number, Player>();
		if (playersData?.players) {
			for (const p of playersData.players) {
				map.set(p.id, p);
			}
		}
		return map;
	}, [playersData?.players]);

	const hasNoTransactions = data && data.totalTransactions === 0;

	return (
		<PageLayout>
			<div className={styles.page}>
				{isLoading && !data && <MarketTrendsSkeleton />}

				{isError && !data && (
					<div className={styles.errorState} role="alert" aria-live="assertive">
						<div className={styles.errorIconTile}>
							<TriangleAlert className={styles.errorIcon} aria-hidden="true" />
						</div>
						<h3 className={styles.errorTitle}>
							Error al cargar tendencias del mercado
						</h3>
						<p className={styles.errorDescription}>
							No fue posible obtener los datos de sentimiento y transacciones en
							tiempo real. Por favor, verifica tu conexión o reintenta la
							consulta.
						</p>
						<Button
							variant="primary"
							onClick={() => void refetch()}
							className={styles.retryButton}
						>
							Reintentar consulta
						</Button>
					</div>
				)}

				{hasNoTransactions && <MarketTrendsEmptyState />}

				{data && !hasNoTransactions && (
					<main className={styles.content}>
						{/* Fila 1: Cockpit Header Estilo Historial de Transacciones */}
						<header className={styles.header}>
							<div className={styles.header__left}>
								<div className={styles.header__iconWrapper}>
									<TrendingUp
										aria-hidden="true"
										className={styles.header__icon}
									/>
								</div>
								<div className={styles.header__titleWrapper}>
									<h1 className={styles.header__title}>
										Tendencias del Mercado
									</h1>
									<p className={styles.header__description}>
										Métricas macroeconómicas y volumen en tiempo real (24h).
									</p>
								</div>
							</div>
							<div className={styles.header__metrics}>
								<MarketMacroMetrics
									totalTradedCredits={data.totalTradedCredits}
									totalTradedTokens={data.totalTradedTokens}
									totalTransactions={data.totalTransactions}
								/>
							</div>
						</header>

						{/* Fila 2: Análisis Triple (Sentimiento Colectivo | Demanda Táctica | Flujo por Liga) */}
						<section
							className={styles.chartsRow}
							aria-label="Sentimiento de mercado, demanda táctica y flujo por competencia"
						>
							<div className={styles.chartCol}>
								<MarketSentimentGauge
									buyPercentage={data.buyTransactionsPercentage}
									sellPercentage={data.sellTransactionsPercentage}
									bias={data.marketBias}
								/>
							</div>
							<div className={styles.chartCol}>
								<MarketPositionalDemandChart
									topHotPlayers={data.topHotPlayers}
									playerMap={playerMap}
								/>
							</div>
							<div className={styles.chartCol}>
								<MarketLeagueDemandChart
									topHotPlayers={data.topHotPlayers}
									playerMap={playerMap}
								/>
							</div>
						</section>

						{/* Fila 3: Tablas de Activos y Mercado (Activos en Tendencia | Mayores Cotizaciones) */}
						<section
							className={styles.tablesRow}
							aria-label="Activos en tendencia y mayores cotizaciones del mercado"
						>
							<div className={styles.hotAssetsCol}>
								<MarketHotAssets
									topHotPlayers={data.topHotPlayers}
									totalTradedTokens={data.totalTradedTokens}
									playerMap={playerMap}
									quotesByPlayerId={quotesQuery.quotesByPlayerId}
									onBuyClick={buyModal.openBuyModal}
								/>
							</div>
							<div className={styles.topValuedCol}>
								<MarketTopValuedPlayers
									playerMap={playerMap}
									quotesByPlayerId={quotesQuery.quotesByPlayerId}
									onBuyClick={buyModal.openBuyModal}
								/>
							</div>
						</section>
					</main>
				)}

				<BuyTokenModal
					isOpen={buyModal.isModalOpen}
					player={buyModal.selectedPlayer}
					quote={buyModal.selectedQuote}
					confirmationBlockReason={buyModal.confirmationBlockReason}
					estimation={buyModal.estimation}
					formRegister={buyModal.formRegister}
					formError={buyModal.formError}
					quantity={buyModal.quantity}
					onQuantityChange={buyModal.onQuantityChange}
					isBusy={buyModal.isBusy}
					canConfirm={buyModal.canConfirm}
					isSuccess={buyModal.isSuccess}
					purchaseResult={buyModal.purchaseResult}
					errorMessage={buyModal.errorMessage}
					onClose={buyModal.closeBuyModal}
					onConfirm={buyModal.handleConfirmBuy}
					triggerRef={buyModal.triggerRef}
					initialFocusRef={buyModal.initialFocusRef}
				/>
			</div>
		</PageLayout>
	);
}
