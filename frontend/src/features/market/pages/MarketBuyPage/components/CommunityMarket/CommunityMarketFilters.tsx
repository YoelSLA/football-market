import type {
	CommunityMarketSort,
	CommunityMarketFilters as FilterModel,
} from "@/features/market/types";
import { Button } from "@/shared/components";
import styles from "./CommunityMarketFilters.module.scss";

interface Props {
	filters: FilterModel;
	leagues: string[];
	positions: string[];
	onChange: (next: Partial<FilterModel>) => void;
	onClear: () => void;
}

export default function CommunityMarketFilters({
	filters,
	leagues,
	positions,
	onChange,
	onClear,
}: Props) {
	return (
		<aside
			className={styles.filters}
			aria-label="Filtros de ofertas de usuarios"
		>
			<h2>Filtros</h2>
			<label>
				Buscar jugador o club
				<input
					type="search"
					value={filters.search}
					onChange={(event) => onChange({ search: event.target.value })}
				/>
			</label>
			<label>
				Liga
				<select
					value={filters.league}
					onChange={(event) => onChange({ league: event.target.value })}
				>
					<option value="all">Todas</option>
					{leagues.map((league) => (
						<option key={league} value={league}>
							{league}
						</option>
					))}
				</select>
			</label>
			<label>
				Posición
				<select
					value={filters.position}
					onChange={(event) => onChange({ position: event.target.value })}
				>
					<option value="all">Todas</option>
					{positions.map((position) => (
						<option key={position} value={position}>
							{position}
						</option>
					))}
				</select>
			</label>
			<label>
				Orden
				<select
					value={filters.sort}
					onChange={(event) =>
						onChange({ sort: event.target.value as CommunityMarketSort })
					}
				>
					<option value="price">Mejor precio</option>
					<option value="quantity">Mayor cantidad ofrecida</option>
					<option value="name">Nombre</option>
				</select>
			</label>
			<Button className={styles.filters__clear} type="button" onClick={onClear}>
				Limpiar filtros
			</Button>
		</aside>
	);
}
