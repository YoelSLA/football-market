import { useState } from "react";
import { PageLayout } from "@/app/layouts";
import { PageHeading } from "@/shared/components";
import {
	DashboardEmptyState,
	DashboardSkeleton,
	FinancialHeroCockpit,
	LiveActivityFeed,
	MarketMoversTable,
	MarketTicker,
	P2PBestDealsWidget,
	PerformanceBenchmarkChart,
	PortfolioAllocationChart,
	QuickActionsBar,
	SorareShowcaseGrid,
} from "../../components";
import {
	useDashboardOverviewQuery,
	useDashboardPerformanceQuery,
} from "../../hooks";
import styles from "./DashboardPage.module.scss";

export default function DashboardPage() {
	const [performanceRange, setPerformanceRange] = useState<"1M" | "3M" | "ALL">(
		"1M",
	);

	const {
		data: overview,
		isLoading: isOverviewLoading,
		isError: isOverviewError,
		error: overviewError,
		refetch: refetchOverview,
	} = useDashboardOverviewQuery();

	const { data: performance, isLoading: isPerformanceLoading } =
		useDashboardPerformanceQuery(performanceRange);

	if (isOverviewLoading) {
		return (
			<PageLayout>
				<DashboardSkeleton />
			</PageLayout>
		);
	}

	if (isOverviewError || !overview) {
		return (
			<PageLayout>
				<div className={styles.errorContainer} role="alert">
					<div className={styles.errorCard}>
						<span className={styles.errorIcon}>⚠️</span>
						<h2 className={styles.errorTitle}>
							Error al sincronizar el mercado
						</h2>
						<p className={styles.errorMessage}>
							{overviewError instanceof Error
								? overviewError.message
								: "No fue posible cargar el resumen ejecutivo del dashboard en este momento."}
						</p>
						<button
							type="button"
							className={styles.retryButton}
							onClick={() => refetchOverview()}
						>
							🔄 Reintentar conexión
						</button>
					</div>
				</div>
			</PageLayout>
		);
	}

	const combinedMovers = [...overview.topGainers, ...overview.topLosers];
	const isNewUser =
		overview.financialSummary.totalTokens === 0 &&
		overview.recentActivity.length === 0;

	return (
		<div className={styles.dashboardContainer}>
			{/* Ribbon Ticker continuo en la parte superior */}
			<MarketTicker
				movers={combinedMovers}
				marketPulse={overview.marketPulse}
			/>

			<PageLayout>
				<div className={styles.pageContent}>
					<PageHeading
						title="Dashboard Ejecutivo"
						description="Supervisión financiera, analytics de cartera y pulso de mercado en tiempo real"
					/>

					{isNewUser ? (
						<DashboardEmptyState
							availableCredits={overview.financialSummary.availableCredits}
						/>
					) : (
						<>
							{/* Hero Cockpit Patrimonial */}
							<FinancialHeroCockpit
								summary={overview.financialSummary}
								sparklineData={performance?.userPortfolioSeries}
							/>

							{/* Barra de Accesos Rápidos */}
							<QuickActionsBar />

							{/* Fila Media: Donut de Asignación + Benchmark FMI */}
							<div className={styles.analyticsRow}>
								<div className={styles.donutCol}>
									<PortfolioAllocationChart allocation={overview.allocation} />
								</div>
								<div className={styles.benchmarkCol}>
									<PerformanceBenchmarkChart
										data={performance}
										activeRange={performanceRange}
										onRangeChange={setPerformanceRange}
										isLoading={isPerformanceLoading}
									/>
								</div>
							</div>

							{/* Showcase Deportivo Sorare */}
							<SorareShowcaseGrid
								valuePicks={overview.valuePicks}
								topGainers={overview.topGainers}
							/>

							{/* Fila Inferior: Oportunidades P2P + Market Movers */}
							<div className={styles.marketRow}>
								<div className={styles.p2pCol}>
									<P2PBestDealsWidget deals={overview.bestP2PDeals} />
								</div>
								<div className={styles.moversCol}>
									<MarketMoversTable
										topGainers={overview.topGainers}
										topLosers={overview.topLosers}
									/>
								</div>
							</div>

							{/* Historial de actividad reciente */}
							<LiveActivityFeed activities={overview.recentActivity} />
						</>
					)}
				</div>
			</PageLayout>
		</div>
	);
}
