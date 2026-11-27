import {
	FilterSearch,
	FilterSelect,
	type FilterSelectOption,
} from "@/shared/components";
import styles from "./MarketFilterFields.module.scss";

interface Props {
	scope: string;
	search: string;
	status: string;
	sort: string;
	onSearchChange: (value: string) => void;
	onStatusChange: (value: string) => void;
	onSortChange: (value: string) => void;
}

const STATUS_OPTIONS: FilterSelectOption[] = [
	{ value: "all", label: "Todas las órdenes" },
	{ value: "ACTIVE", label: "Activas" },
	{ value: "SOLD", label: "Vendidas" },
	{ value: "CANCELLED", label: "Canceladas" },
	{ value: "EXPIRED", label: "Expiradas" },
];

const SORT_OPTIONS: FilterSelectOption[] = [
	{ value: "NEWEST", label: "Más recientes" },
	{ value: "OLDEST", label: "Más antiguas" },
	{ value: "PRICE_DESC", label: "Mayor precio" },
	{ value: "PRICE_ASC", label: "Menor precio" },
];

export default function OrdersMarketFilterFields({
	scope,
	search,
	status,
	sort,
	onSearchChange,
	onStatusChange,
	onSortChange,
}: Props) {
	return (
		<div className={styles.fields}>
			<FilterSearch
				id={`${scope}-search`}
				className={styles.search}
				value={search}
				onChange={onSearchChange}
				label="Buscar jugador o club"
				placeholder="Buscar jugador o club…"
			/>
			<FilterSelect
				id={`${scope}-status`}
				className={styles.select}
				label="Estado"
				value={status || "all"}
				options={STATUS_OPTIONS}
				onChange={onStatusChange}
			/>
			<FilterSelect
				id={`${scope}-sort`}
				className={styles.select}
				label="Orden"
				value={sort || "NEWEST"}
				options={SORT_OPTIONS}
				onChange={onSortChange}
			/>
		</div>
	);
}
