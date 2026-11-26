import {
	TransactionHistorySort,
	TransactionType,
} from "@/features/portfolio/types";
import {
	Button,
	FilterSearch,
	FilterSelect,
	type FilterSelectOption,
} from "@/shared/components";
import { CalendarDays, X } from "@/shared/icons";
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

const TYPE_OPTIONS: FilterSelectOption[] = [
	{ value: "ALL", label: "Tipo: Todas" },
	{ value: TransactionType.BUY, label: "Tipo: Compras" },
	{ value: TransactionType.SELL, label: "Tipo: Ventas" },
];

const SORT_OPTIONS: FilterSelectOption[] = [
	{ value: TransactionHistorySort.NEWEST, label: "Orden: Más recientes" },
	{ value: TransactionHistorySort.OLDEST, label: "Orden: Más antiguas" },
	{ value: TransactionHistorySort.AMOUNT_DESC, label: "Orden: Mayor importe" },
	{ value: TransactionHistorySort.AMOUNT_ASC, label: "Orden: Menor importe" },
];

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
		<search aria-label="Filtrar transacciones" className={styles.toolbar}>
			<div className={styles.toolbar__left}>
				<FilterSearch
					id="tx-filter-search"
					className={styles.toolbar__search}
					label="Buscar jugador"
					accessibleLabel="Buscar jugador por nombre"
					placeholder="Buscar jugador…"
					value={search}
					onChange={(value) => onChange("search", value)}
				/>
				<FilterSelect
					id="tx-filter-type"
					className={styles.toolbar__select}
					label="Tipo de operación"
					value={type ?? "ALL"}
					options={TYPE_OPTIONS}
					onChange={(value) => onChange("type", value === "ALL" ? "" : value)}
				/>
				<div
					className={styles.toolbar__dateRange}
					title="Filtrar por rango de fechas"
				>
					<CalendarDays
						aria-hidden="true"
						className={styles.toolbar__dateIcon}
					/>
					<span className={styles.toolbar__datePrefix}>Desde</span>
					<input
						type="date"
						value={from}
						max={to || undefined}
						aria-label="Fecha desde"
						className={styles.toolbar__dateInput}
						onChange={(event) => onChange("from", event.target.value)}
					/>
					<span className={styles.toolbar__dateSeparator}>–</span>
					<span className={styles.toolbar__datePrefix}>Hasta</span>
					<input
						type="date"
						value={to}
						min={from || undefined}
						aria-label="Fecha hasta"
						className={styles.toolbar__dateInput}
						onChange={(event) => onChange("to", event.target.value)}
					/>
				</div>
				<FilterSelect
					id="tx-filter-sort"
					className={styles.toolbar__sort}
					label="Ordenar por"
					value={sort}
					options={SORT_OPTIONS}
					onChange={(value) => onChange("sort", value)}
				/>
			</div>
			{hasFilters && (
				<div className={styles.toolbar__right}>
					<Button
						type="button"
						className={styles.toolbar__clear}
						onClick={onClear}
						title="Restablecer todos los filtros"
					>
						<X aria-hidden="true" />
						<span>Limpiar</span>
					</Button>
				</div>
			)}
		</search>
	);
}
