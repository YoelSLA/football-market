import { useMemo } from "react";
import { PageLayout } from "@/app/layouts";
import { useMarketTrendsQuery } from "@/features/market/hooks";
import { useAllPlayersQuery } from "@/features/players";
import type { Player } from "@/features/players/types";
import { Button } from "@/shared/components";
import { TriangleAlert } from "@/shared/icons";
import MarketDemandCurveChart from "./components/MarketDemandCurveChart";
import MarketDemandShareChart from "./components/MarketDemandShareChart";
import MarketHotAssets from "./components/MarketHotAssets";
import MarketMacroMetrics from "./components/MarketMacroMetrics";
import MarketPositionalDemandChart from "./components/MarketPositionalDemandChart";
import MarketSentimentBar from "./components/MarketSentimentBar";
import MarketSentimentGauge from "./components/MarketSentimentGauge";
import MarketSupplyDemandBalance from "./components/MarketSupplyDemandBalance";
import MarketTrendsEmptyState from "./components/MarketTrendsEmptyState";
import MarketTrendsHeader from "./components/MarketTrendsHeader";
import MarketTrendsSkeleton from "./components/MarketTrendsSkeleton";
import styles from "./MarketTrendsPage.module.scss";

/**
 * Tablero de Tendencias y Sentimiento del Mercado (SPEC-029).
 * Visualiza la presión compradora/vendedora macro de la comunidad,
 * el tacómetro de sentimiento radial, análisis de oferta y demanda,
 * curva de elasticidad, cuota de demanda del Top 5, demanda táctica
 * por demarcación y ranking de activos calientes en las últimas 24 horas.
 */
export default function MarketTrendsPage() {
	const { data, isLoading, isError, isFetching, refetch } =
		useMarketTrendsQuery();
	const { data: playersData } = useAllPlayersQuery();

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
				<MarketTrendsHeader
					startsAt={data?.window?.startsAt}
					endsAt={data?.window?.endsAt}
					isFetching={isFetching}
					onRefresh={() => void refetch()}
				/>

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
						{/* Fila 1: Indicadores de Sentimiento (Tacómetro Radial + Barra Proporcional) */}
						<section
							className={styles.sentimentRow}
							aria-label="Indicadores de sentimiento"
						>
							<MarketSentimentGauge
								buyPercentage={data.buyTransactionsPercentage}
								bias={data.marketBias}
							/>
							<MarketSentimentBar
								buyPercentage={data.buyTransactionsPercentage}
								sellPercentage={data.sellTransactionsPercentage}
								bias={data.marketBias}
								totalTransactions={data.totalTransactions}
							/>
						</section>

						{/* Fila 2: Métricas Cuantitativas Macro */}
						<section aria-label="Métricas macro del mercado">
							<MarketMacroMetrics
								totalTradedCredits={data.totalTradedCredits}
								totalTradedTokens={data.totalTradedTokens}
								totalTransactions={data.totalTransactions}
								topPlayerName={data.topHotPlayers[0]?.playerName}
							/>
						</section>

						{/* Fila 3: Análisis Avanzado de Oferta y Demanda */}
						<section
							className={styles.supplyDemandRow}
							aria-label="Balance y elasticidad de oferta y demanda"
						>
							<MarketSupplyDemandBalance
								buyPercentage={data.buyTransactionsPercentage}
								sellPercentage={data.sellTransactionsPercentage}
								totalTransactions={data.totalTransactions}
								totalTradedCredits={data.totalTradedCredits}
								totalTradedTokens={data.totalTradedTokens}
							/>
							<MarketDemandCurveChart
								buyPercentage={data.buyTransactionsPercentage}
								bias={data.marketBias}
							/>
						</section>

						{/* Fila 4: Segmentación de Demanda y Demarcación Táctica */}
						{data.topHotPlayers.length > 0 && (
							<section
								className={styles.analysisRow}
								aria-label="Cuota de demanda y distribución táctica"
							>
								<MarketDemandShareChart
									topHotPlayers={data.topHotPlayers}
									totalTradedTokens={data.totalTradedTokens}
									playerMap={playerMap}
								/>
								<MarketPositionalDemandChart
									topHotPlayers={data.topHotPlayers}
									playerMap={playerMap}
								/>
							</section>
						)}

						{/* Fila 5: Tarjetas Interactivas de Activos en Tendencia */}
						{data.topHotPlayers.length > 0 && (
							<MarketHotAssets
								topHotPlayers={data.topHotPlayers}
								playerMap={playerMap}
							/>
						)}
					</main>
				)}
			</div>
		</PageLayout>
	);
}
