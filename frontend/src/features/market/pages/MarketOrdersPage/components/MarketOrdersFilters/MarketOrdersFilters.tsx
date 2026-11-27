import {
	Button,
	FilterSearch,
	FilterSelect,
	type FilterSelectOption,
} from "@/shared/components";
import { X } from "@/shared/icons";
import styles from "./MarketOrdersFilters.module.scss";

interface Props {
	search: string;
	status: string;
	sort: string;
	hasFilters: boolean;
	onSearchChange: (value: string) => void;
	onStatusChange: (value: string) => void;
	onSortChange: (value: string) => void;
	onClear: () => void;
}

const STATUS_OPTIONS: FilterSelectOption[] = [
	{ value: "ALL", label: "Estado: Todas" },
	{ value: "ACTIVE", label: "Estado: Activas" },
	{ value: "SOLD", label: "Estado: Vendidas" },
	{ value: "CANCELLED", label: "Estado: Canceladas" },
	{ value: "EXPIRED", label: "Estado: Expiradas" },
];

const SORT_OPTIONS: FilterSelectOption[] = [
	{ value: "NEWEST", label: "Orden: Más recientes" },
	{ value: "OLDEST", label: "Orden: Más antiguas" },
	{ value: "PRICE_DESC", label: "Orden: Mayor precio" },
	{ value: "PRICE_ASC", label: "Orden: Menor precio" },
];

export default function MarketOrdersFilters({
	search,
	status,
	sort,
	hasFilters,
	onSearchChange,
	onStatusChange,
	onSortChange,
	onClear,
}: Props) {
	return (
		<search aria-label="Filtrar órdenes de venta" className={styles.toolbar}>
			<div className={styles.toolbar__left}>
				<FilterSearch
					id="orders-filter-search"
					className={styles.toolbar__search}
					label="Buscar jugador"
					accessibleLabel="Buscar jugador por nombre o club"
					placeholder="Buscar jugador…"
					value={search}
					onChange={onSearchChange}
				/>
				<FilterSelect
					id="orders-filter-status"
					className={styles.toolbar__select}
					label="Estado"
					value={status || "ALL"}
					options={STATUS_OPTIONS}
					onChange={(val) => onStatusChange(val === "ALL" ? "" : val)}
				/>
				<FilterSelect
					id="orders-filter-sort"
					className={styles.toolbar__sort}
					label="Ordenar por"
					value={sort || "NEWEST"}
					options={SORT_OPTIONS}
					onChange={onSortChange}
				/>
			</div>
			{hasFilters && (
				<div className={styles.toolbar__right}>
					<Button
						type="button"
						className={styles.toolbar__clear}
						onClick={onClear}
						title="Restablecer filtros"
					>
						<X aria-hidden="true" />
						<span>Limpiar</span>
					</Button>
				</div>
			)}
		</search>
	);
}
