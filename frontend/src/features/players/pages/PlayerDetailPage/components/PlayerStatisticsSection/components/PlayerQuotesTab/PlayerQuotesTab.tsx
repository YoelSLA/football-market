import type { QueryState, StatisticsTab } from "@/features/players/types";
import { Button } from "@/shared/components";
import styles from "./PlayerQuotesTab.module.scss";

interface Props {
	activeTab: StatisticsTab;
	quotesState: QueryState;
	quotesErrorMessage?: string | null;
	onRetryQuotes?: () => void;
}

export default function PlayerQuotesTab({
	activeTab,
	quotesState,
	quotesErrorMessage,
	onRetryQuotes,
}: Props) {
	return (
		<div
			id="panel-quotes"
			role="tabpanel"
			aria-labelledby="tab-quotes"
			hidden={activeTab !== "quotes"}
			aria-busy={quotesState === "loading"}
			className={styles.quotesTab}
		>
			{quotesState === "loading" ? (
				<div className={styles.quotesTab__loading} role="status">
					<p>Consultando cotizaciones del jugador…</p>
				</div>
			) : quotesState === "error" ? (
				<div className={styles.quotesTab__errorPlaceholder} role="alert">
					<p>
						{quotesErrorMessage ||
							"No se pudieron cargar las cotizaciones del jugador."}
					</p>
					{onRetryQuotes && (
						<Button type="button" onClick={onRetryQuotes}>
							Reintentar
						</Button>
					)}
				</div>
			) : (
				<div className={styles.quotesTab__placeholder}>
					{/* Preparado para los componentes de Fase 3: PlayerQuotesTab */}
				</div>
			)}
		</div>
	);
}
