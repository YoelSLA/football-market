import { useState } from "react";
import { PageLayout } from "@/app/layouts";
import {
	Activity,
	LayoutDashboard,
	RotateCw,
	TriangleAlert,
	Wallet,
} from "@/shared/icons";
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

	const formatCredits = (amount: number | null) => {
		if (amount === null) return "—";
		return `${amount.toLocaleString("es-ES", {
			minimumFractionDigits: 2,
			maximumFractionDigits: 2,
		})} ₢`;
	};

	if (isOverviewLoading) {
		return (
			<PageLayout>
				<div className={styles.dashboardPage}>
					<DashboardSkeleton />
				</div>
			</PageLayout>
		);
	}

	if (isOverviewError || !overview) {
		return (
			<PageLayout>
				<div className={styles.dashboardPage}>
					<div className={styles.errorContainer} role="alert">
						<div className={styles.errorCard}>
							<span className={styles.errorIcon} aria-hidden="true">
								<TriangleAlert size={36} />
							</span>
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
								<RotateCw size={14} aria-hidden="true" />
								<span>Reintentar conexión</span>
							</button>
						</div>
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
		<PageLayout>
			<div className={styles.dashboardPage}>
				<div className={styles.dashboardPage__scrollRegion}>
					{/* Header de página con identidad Football Market */}
					<header className={styles.header}>
						<div className={styles.headerTitleGroup}>
							<span className={styles.headerIcon} aria-hidden="true">
								<LayoutDashboard />
							</span>
							<div className={styles.headerText}>
								<h1 className={styles.title}>Dashboard Ejecutivo</h1>
								<p className={styles.description}>
									Supervisión financiera, analytics de cartera y pulso de
									mercado en tiempo real
								</p>
							</div>
						</div>

						<div className={styles.headerMetrics}>
							<div className={styles.headerMetricItem}>
								<span className={styles.metricTile} aria-hidden="true">
									<Wallet />
								</span>
								<div className={styles.metricContent}>
									<span className={styles.metricLabel}>Saldo disponible</span>
									<strong className={styles.metricValue}>
										{formatCredits(overview.financialSummary.availableCredits)}
									</strong>
								</div>
							</div>

							<div className={styles.headerMetricItem}>
								<span className={styles.metricTile} aria-hidden="true">
									<Activity />
								</span>
								<div className={styles.metricContent}>
									<span className={styles.metricLabel}>Cap. del Mercado</span>
									<strong className={styles.metricValue}>
										{formatCredits(overview.marketPulse.totalMarketCap)}
									</strong>
								</div>
							</div>
						</div>
					</header>

					{/* Ribbon Ticker continuo */}
					<MarketTicker
						movers={combinedMovers}
						marketPulse={overview.marketPulse}
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
			</div>
		</PageLayout>
	);
}
