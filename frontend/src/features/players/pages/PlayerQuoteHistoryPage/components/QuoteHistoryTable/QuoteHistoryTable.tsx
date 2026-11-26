import type { ReactNode } from "react";
import type { PlayerQuote } from "@/features/players/types";
import styles from "./QuoteHistoryTable.module.scss";
import { QuoteHistoryTableRow } from "./QuoteHistoryTableRow";

interface Props {
	quotes: PlayerQuote[];
	toolbar?: ReactNode;
	isLoading?: boolean;
	errorMessage?: string | null;
	onRetry?: () => void;
	onClearFilters?: () => void;
}

export default function QuoteHistoryTable({
	quotes,
	toolbar,
	isLoading,
	errorMessage,
	onRetry,
	onClearFilters,
}: Props) {
	return (
		<div className={styles.tableWrapper}>
			{toolbar && <div className={styles.tableWrapper__toolbar}>{toolbar}</div>}

			<div className={styles.tableWrapper__container}>
				{isLoading ? (
					<div className={styles.emptyState}>
						<p className={styles.emptyState__text}>
							Consultando serie de cotizaciones oficiales…
						</p>
					</div>
				) : errorMessage ? (
					<div className={styles.emptyState}>
						<p className={styles.emptyState__text}>{errorMessage}</p>
						{onRetry && (
							<button
								type="button"
								className={styles.emptyState__btn}
								onClick={onRetry}
							>
								Reintentar
							</button>
						)}
					</div>
				) : quotes.length === 0 ? (
					<div className={styles.emptyState}>
						<p className={styles.emptyState__text}>
							No se encontraron cotizaciones con los filtros seleccionados.
						</p>
						{onClearFilters && (
							<button
								type="button"
								className={styles.emptyState__btn}
								onClick={onClearFilters}
							>
								Restablecer filtros
							</button>
						)}
					</div>
				) : (
					<table
						className={styles.table}
						aria-label="Historial cronológico de cotizaciones"
					>
						<thead>
							<tr>
								<th scope="col" className={styles.th_date}>
									Fecha
								</th>
								<th scope="col" className={styles.th_prevPrice}>
									Precio Anterior
								</th>
								<th scope="col" className={styles.th_price}>
									Precio Oficial
								</th>
								<th scope="col" className={styles.th_netChange}>
									Cambio Neto
								</th>
								<th scope="col" className={styles.th_variation}>
									Variación
								</th>
								<th scope="col" className={styles.th_demand}>
									Demanda Neta
								</th>
							</tr>
						</thead>
						<tbody>
							{quotes.map((quote) => (
								<QuoteHistoryTableRow
									key={`${quote.effectiveDate}-${quote.calculatedAt ?? quote.price}`}
									quote={quote}
								/>
							))}
						</tbody>
					</table>
				)}
			</div>
		</div>
	);
}
