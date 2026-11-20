import type {
	PlayerQuoteHistory,
	QueryState,
	StatisticsTab,
} from "@/features/players/types";
import { Button } from "@/shared/components";
import PlayerQuoteEvolutionChart from "../../../PlayerQuoteEvolutionChart";
import PlayerQuotePanel from "../../../PlayerQuotePanel";
import styles from "./PlayerQuotesTab.module.scss";

interface Props {
	activeTab: StatisticsTab;
	quotes?: PlayerQuoteHistory;
	quotesState: QueryState;
	quotesErrorMessage?: string | null;
	onRetryQuotes: () => void;
}

export default function PlayerQuotesTab({
	activeTab,
	quotes,
	quotesState,
	quotesErrorMessage,
	onRetryQuotes,
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
				<>
					<PlayerQuotePanel snapshot={series?.snapshot} />
					<PlayerQuoteEvolutionChart series={series} />
				</>
			)}
		</div>
	);
}
