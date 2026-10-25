import { Search, ShoppingCart, SlidersHorizontal } from "@/shared/icons";
import styles from "./MarketFilters.module.scss";

export default function MarketFilters() {
	const renderFields = (scope: string) => (
			<fieldset className={styles.marketFilters__fields} disabled aria-label="Filtros pendientes">
				<label className={styles.marketFilters__search}>
					<Search aria-hidden="true" />
					<input aria-label="Buscar jugador o club (pendiente)" placeholder="Buscar jugador o club..." />
				</label>
				<label>Liga<select defaultValue="all"><option value="all">Todas las ligas</option></select></label>
				<label>Posición<select defaultValue="all"><option value="all">Todas las posiciones</option></select></label>
				<label>Precio actual<select defaultValue="all"><option value="all">Todos los precios</option></select></label>
				<label>Ordenar por<select defaultValue="price"><option value="price">Precio actual</option></select></label>
				<div className={styles.marketFilters__direction}>
					<span>Dirección</span>
					<label><input type="radio" name={`${scope}-price-direction`} defaultChecked /> Menor a mayor</label>
					<label><input type="radio" name={`${scope}-price-direction`} /> Mayor a menor</label>
				</div>
				<button type="button" className={styles.marketFilters__apply}><ShoppingCart aria-hidden="true" /> Aplicar filtros</button>
			</fieldset>
	);

	return (
		<aside className={styles.marketFilters} aria-label="Filtros del mercado, pendientes de implementación">
			<div className={styles.marketFilters__desktop}>
				<div className={styles.marketFilters__heading}>
					<h2><SlidersHorizontal aria-hidden="true" /> Filtros</h2>
					<button type="button" disabled>Limpiar filtros</button>
				</div>
				<p className={styles.marketFilters__pending}>Pendiente · controles solo visuales</p>
				{renderFields("market-desktop")}
			</div>
			<details className={styles.marketFilters__mobile}>
				<summary><SlidersHorizontal aria-hidden="true" /> Filtros <span>Pendientes · desplegar</span></summary>
				<div className={styles.marketFilters__popover}>
					<div className={styles.marketFilters__heading}>
						<h2>Filtros</h2>
						<button type="button" disabled>Limpiar filtros</button>
					</div>
					<p className={styles.marketFilters__pending}>Pendiente · controles solo visuales</p>
					{renderFields("market-mobile")}
				</div>
			</details>
		</aside>
	);
}
