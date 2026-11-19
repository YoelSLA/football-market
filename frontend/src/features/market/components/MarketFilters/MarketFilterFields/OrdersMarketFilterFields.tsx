import { FilterSearch, FilterSelect } from "@/shared/components";
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

const STATUS_OPTIONS = [
	{ value: "all", label: "Todas las órdenes" },
	{ value: "ACTIVE", label: "Activas" },
	{ value: "SOLD", label: "Vendidas" },
	{ value: "CANCELLED", label: "Canceladas" },
	{ value: "EXPIRED", label: "Expiradas" },
];

const SORT_OPTIONS = [
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
		<fieldset
			className={`${styles.marketFilterFields} ${styles.marketFilterFields_orders}`}
		>
			<FilterSearch
				id={`${scope}-search`}
				className={styles.marketFilterFields__search}
				value={search}
				onChange={onSearchChange}
				label="Buscar jugador o club"
				placeholder="Nombre o club..."
			/>
			<FilterSelect
				id={`${scope}-status`}
				className={styles.marketFilterFields__status}
				label="Estado"
				value={status || "all"}
				options={STATUS_OPTIONS}
				onChange={(value) => onStatusChange(value === "all" ? "" : value)}
			/>
			<FilterSelect
				id={`${scope}-sort`}
				className={styles.marketFilterFields__sort}
				label="Ordenar por"
				value={sort || "NEWEST"}
				options={SORT_OPTIONS}
				onChange={onSortChange}
			/>
		</fieldset>
	);
}
