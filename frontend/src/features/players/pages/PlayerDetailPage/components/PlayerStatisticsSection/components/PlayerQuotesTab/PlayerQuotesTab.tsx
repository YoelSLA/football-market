import type {
	PlayerQuoteHistory,
	QueryState,
	StatisticsTab,
} from "@/features/players/types";
import { Button } from "@/shared/components";
import PlayerFactorsEvolutionChart from "../../../PlayerFactorsEvolutionChart";
import PlayerP2PTeaserCard from "../../../PlayerP2PTeaserCard";
import PlayerQuoteEvolutionChart from "../../../PlayerQuoteEvolutionChart";
import PlayerQuotePanel from "../../../PlayerQuotePanel";
import PlayerRecentQuotesCard from "../../../PlayerRecentQuotesCard";
import styles from "./PlayerQuotesTab.module.scss";

interface Props {
	activeTab: StatisticsTab;
	quotes?: PlayerQuoteHistory;
	quotesState: QueryState;
	quotesErrorMessage?: string | null;
	onRetryQuotes: () => void;
	playerName?: string;
	playerId: number;
}

export default function PlayerQuotesTab({
	activeTab,
	quotes,
	quotesState,
	quotesErrorMessage,
	onRetryQuotes,
	playerName = "este jugador",
	playerId,
}: Props) {
	const current = quotes?.currentQuote;
	const series = quotes
		? {
				playerId: quotes.playerId,
				points: quotes.quotes.map((quote) => ({
					playerId: quotes.playerId,
					date: quote.effectiveDate,
					price: quote.price,
					isDemo: true as const,
				})),
				snapshot: {
					playerId: quotes.playerId,
					price: current?.price ?? null,
					date: current?.effectiveDate ?? null,
					absoluteChange: current?.absoluteChange ?? null,
					percentageChange: current?.percentageChange ?? null,
					direction: current?.direction ?? "unknown",
					isDemo: true as const,
				},
			}
		: undefined;

	return (
		<div
			id="panel-quotes"
			role="tabpanel"
			aria-labelledby="tab-quotes"
			hidden={activeTab !== "quotes"}
			aria-busy={quotesState === "loading"}
			className={styles.playerQuotesTab}
		>
			{quotesState === "loading" ? (
				<p role="status">Cargando cotizaciones oficiales…</p>
			) : quotesState === "error" ? (
				<section role="alert">
					<p>{quotesErrorMessage ?? "Error al cargar cotizaciones."}</p>
					<Button type="button" onClick={onRetryQuotes}>
						Reintentar
					</Button>
				</section>
			) : (
				<div className={styles.playerQuotesTab__layout}>
					{/* 1. Dos gráficos gemelos: Cotización Oficial y Factores Híbridos (Deporte vs Mercado) */}
					<div className={styles.playerQuotesTab__chartsGrid}>
						<PlayerQuoteEvolutionChart series={series} />
						<PlayerFactorsEvolutionChart quotes={quotes} />
					</div>

					{/* 2. Dos columnas gemelas equilibradas (Cero espacio muerto) */}
					<div className={styles.playerQuotesTab__columns}>
						<div className={styles.playerQuotesTab__col}>
							<PlayerRecentQuotesCard
								quotes={quotes}
								playerId={quotes?.playerId ?? playerId}
							/>
						</div>
						<div className={styles.playerQuotesTab__col}>
							<PlayerQuotePanel snapshot={series?.snapshot} quote={current} />
							<PlayerP2PTeaserCard playerName={playerName} quote={current} />
						</div>
					</div>
				</div>
			)}
		</div>
	);
}
