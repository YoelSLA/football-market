import { useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { PageLayout } from "@/app/layouts";
import MarketPagination from "@/features/market/components/MarketPagination";
import { usePlayerDetailPage } from "@/features/players/hooks";
import QuoteHistoryFilters, {
	type DirectionFilter,
} from "./components/QuoteHistoryFilters";
import QuoteHistoryHeader from "./components/QuoteHistoryHeader";
import QuoteHistoryTable from "./components/QuoteHistoryTable";
import styles from "./PlayerQuoteHistoryPage.module.scss";

const PAGE_SIZE = 12;

export default function PlayerQuoteHistoryPage() {
	const { id } = useParams();
	const detail = usePlayerDetailPage(id);
	const player = detail.player;
	const allQuotes = detail.quotes?.quotes ?? [];

	const [directionFilter, setDirectionFilter] =
		useState<DirectionFilter>("ALL");
	const [startDate, setStartDate] = useState<string>("");
	const [endDate, setEndDate] = useState<string>("");
	const [currentPage, setCurrentPage] = useState<number>(1);

	// Contadores para las píldoras de variación
	const counts = useMemo(() => {
		let up = 0;
		let down = 0;
		let flat = 0;
		for (const q of allQuotes) {
			const pct = q.percentageChange ?? 0;
			if (q.direction === "up" || pct > 0) up++;
			else if (q.direction === "down" || pct < 0) down++;
			else flat++;
		}
		return { all: allQuotes.length, up, down, flat };
	}, [allQuotes]);

	// Filtrado de cotizaciones
	const filteredQuotes = useMemo(() => {
		return allQuotes.filter((q) => {
			const pct = q.percentageChange ?? 0;
			const isUp = q.direction === "up" || pct > 0;
			const isDown = q.direction === "down" || pct < 0;
			const isFlat = !isUp && !isDown;

			if (directionFilter === "UP" && !isUp) return false;
			if (directionFilter === "DOWN" && !isDown) return false;
			if (directionFilter === "FLAT" && !isFlat) return false;

			if (startDate && q.effectiveDate < startDate) return false;
			if (endDate && q.effectiveDate > endDate) return false;

			return true;
		});
	}, [allQuotes, directionFilter, startDate, endDate]);

	const totalPages = Math.max(1, Math.ceil(filteredQuotes.length / PAGE_SIZE));
	const paginatedQuotes = useMemo(() => {
		const start = (currentPage - 1) * PAGE_SIZE;
		return filteredQuotes.slice(start, start + PAGE_SIZE);
	}, [filteredQuotes, currentPage]);

	// Exportar CSV
	const handleExportCSV = () => {
		if (!player || allQuotes.length === 0) return;

		const headers =
			"Fecha,Precio Anterior (₡),Precio Oficial (₡),Cambio Neto (₡),Variación (%),Demanda Neta (Tokens)\n";
		const rows = allQuotes
			.map(
				(q) =>
					`"${q.effectiveDate}",${q.previousPrice ?? ""},${q.price},${q.absoluteChange ?? 0},${q.percentageChange ?? 0},${q.breakdown?.netDemandTokens ?? 0}`,
			)
			.join("\n");

		const blob = new Blob([headers + rows], {
			type: "text/csv;charset=utf-8;",
		});
		const url = URL.createObjectURL(blob);
		const link = document.createElement("a");
		link.href = url;
		link.setAttribute(
			"download",
			`cotizaciones-${player.name.toLowerCase().replace(/\s+/g, "-")}.csv`,
		);
		document.body.appendChild(link);
		link.click();
		document.body.removeChild(link);
	};

	const handleClearFilters = () => {
		setStartDate("");
		setEndDate("");
		setDirectionFilter("ALL");
		setCurrentPage(1);
	};

	return (
		<PageLayout>
			<section
				className={styles.quoteHistoryPage}
				aria-label={`Historial de cotizaciones de ${player?.name ?? "jugador"}`}
				aria-busy={
					detail.detailState === "loading" || detail.quotesState === "loading"
				}
			>
				{player && (
					<QuoteHistoryHeader
						player={player}
						currentQuote={detail.currentQuote}
						allQuotesCount={allQuotes.length}
						latestQuote={allQuotes[0]}
						isLoading={detail.quotesState === "loading"}
					/>
				)}

				<div className={styles.quoteHistoryPage__scrollRegion}>
					{!player ? (
						<div className={styles.quoteHistoryPage__status}>
							<p>Cargando información del jugador…</p>
						</div>
					) : (
						<QuoteHistoryTable
							quotes={paginatedQuotes}
							isLoading={detail.quotesState === "loading"}
							errorMessage={detail.quotesErrorMessage}
							onRetry={() => detail.retryQuotes()}
							onClearFilters={handleClearFilters}
							toolbar={
								<QuoteHistoryFilters
									directionFilter={directionFilter}
									onDirectionChange={(dir) => {
										setDirectionFilter(dir);
										setCurrentPage(1);
									}}
									startDate={startDate}
									endDate={endDate}
									onStartDateChange={(date) => {
										setStartDate(date);
										setCurrentPage(1);
									}}
									onEndDateChange={(date) => {
										setEndDate(date);
										setCurrentPage(1);
									}}
									onClearFilters={handleClearFilters}
									onExportCSV={handleExportCSV}
									counts={counts}
									canExport={allQuotes.length > 0}
								/>
							}
						/>
					)}
				</div>

				{player && filteredQuotes.length > 0 && (
					<footer className={styles.quoteHistoryPage__footer}>
						<MarketPagination
							page={currentPage - 1}
							totalPages={totalPages}
							visiblePlayers={paginatedQuotes.length}
							totalPlayers={filteredQuotes.length}
							pageSize={PAGE_SIZE}
							showSummary={false}
							variant="official"
							onPageChange={(zeroBasedPage) =>
								setCurrentPage(zeroBasedPage + 1)
							}
						/>
					</footer>
				)}
			</section>
		</PageLayout>
	);
}
