import {
	TransactionHistorySort,
	TransactionType,
} from "@/features/portfolio/types";
import { Button } from "@/shared/components";
import styles from "./TransactionHistoryFilters.module.scss";

interface Props {
	search: string;
	type?: TransactionType;
	from: string;
	to: string;
	sort: TransactionHistorySort;
	hasFilters: boolean;
	onChange: (key: string, value: string) => void;
	onClear: () => void;
}

export default function TransactionHistoryFilters({
	search,
	type,
	from,
	to,
	sort,
	hasFilters,
	onChange,
	onClear,
}: Props) {
	return (
		<search
			aria-label="Filtrar historial"
			className={styles.transactionHistoryFilters}
		>
			<form
				className={styles.transactionHistoryFilters__form}
				onSubmit={(event) => event.preventDefault()}
			>
				<label className={styles.transactionHistoryFilters__search}>
					<span>Buscar jugador</span>
					<input
						type="search"
						value={search}
						placeholder="Buscar jugador…"
						onChange={(event) => onChange("search", event.target.value)}
					/>
				</label>
				<label className={styles.transactionHistoryFilters__field}>
					<span>Tipo</span>
					<select
						value={type ?? ""}
						onChange={(event) => onChange("type", event.target.value)}
					>
						<option value="">Todas</option>
						<option value={TransactionType.BUY}>Compras</option>
						<option value={TransactionType.SELL}>Ventas</option>
					</select>
				</label>
				<label className={styles.transactionHistoryFilters__field}>
					<span>Desde</span>
					<input
						type="date"
						value={from}
						max={to || undefined}
						onChange={(event) => onChange("from", event.target.value)}
					/>
				</label>
				<label className={styles.transactionHistoryFilters__field}>
					<span>Hasta</span>
					<input
						type="date"
						value={to}
						min={from || undefined}
						onChange={(event) => onChange("to", event.target.value)}
					/>
				</label>
				<label className={styles.transactionHistoryFilters__sort}>
					<span>Orden</span>
					<select
						value={sort}
						onChange={(event) => onChange("sort", event.target.value)}
					>
						<option value={TransactionHistorySort.NEWEST}>Más recientes</option>
						<option value={TransactionHistorySort.OLDEST}>Más antiguas</option>
						<option value={TransactionHistorySort.AMOUNT_DESC}>
							Mayor importe
						</option>
						<option value={TransactionHistorySort.AMOUNT_ASC}>
							Menor importe
						</option>
					</select>
				</label>
				<Button
					className={styles.transactionHistoryFilters__clear}
					type="button"
					variant="secondary"
					onClick={onClear}
					disabled={!hasFilters}
				>
					Limpiar
				</Button>
			</form>
		</search>
	);
}
