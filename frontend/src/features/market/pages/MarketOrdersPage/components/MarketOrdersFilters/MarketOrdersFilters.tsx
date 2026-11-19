import styles from "./MarketOrdersFilters.module.scss";

interface Props {
	search: string;
	status: string;
	sort: string;
	onSearchChange: (value: string) => void;
	onStatusChange: (status: string) => void;
	onSortChange: (sort: string) => void;
	onClear: () => void;
}

export default function MarketOrdersFilters({
	search,
	status,
	sort,
	onSearchChange,
	onStatusChange,
	onSortChange,
	onClear,
}: Props) {
	return (
		<section className={styles.ordersFilters} aria-label="Filtros de órdenes">
			<div className={styles.ordersFilters__group}>
				<input
					type="text"
					value={search}
					onChange={(e) => onSearchChange(e.target.value)}
					placeholder="Buscar por jugador o club..."
					className={styles.ordersFilters__search}
					aria-label="Buscar por jugador o club"
				/>

				<select
					value={status}
					onChange={(e) => onStatusChange(e.target.value)}
					className={styles.ordersFilters__select}
					aria-label="Filtrar por estado"
				>
					<option value="">Todas las órdenes</option>
					<option value="ACTIVE">Activas</option>
					<option value="SOLD">Vendidas</option>
					<option value="CANCELLED">Canceladas</option>
					<option value="EXPIRED">Expiradas</option>
				</select>

				<select
					value={sort}
					onChange={(e) => onSortChange(e.target.value)}
					className={styles.ordersFilters__select}
					aria-label="Ordenar publicaciones"
				>
					<option value="NEWEST">Más recientes</option>
					<option value="OLDEST">Más antiguas</option>
					<option value="PRICE_DESC">Mayor precio</option>
					<option value="PRICE_ASC">Menor precio</option>
				</select>
			</div>

			<button
				type="button"
				onClick={onClear}
				className={styles.ordersFilters__clearBtn}
			>
				Limpiar filtros
			</button>
		</section>
	);
}
