import type { RankingFilterCriteria } from "@/features/players/types";
import { X } from "@/shared/icons";
import styles from "./RankingsFilters.module.scss";

interface Props {
	filters: RankingFilterCriteria;
	onFilterChange: (key: keyof RankingFilterCriteria, value: string) => void;
	onClearFilters: () => void;
	availableLeagues: string[];
	totalCount: number;
	filteredCount: number;
}

export default function RankingsFilters({
	filters,
	onFilterChange,
	onClearFilters,
	availableLeagues,
}: Props) {
	const hasActiveFilters =
		Boolean(filters.searchTerm.trim()) ||
		Boolean(filters.selectedLeague) ||
		Boolean(filters.selectedPosition);

	return (
		<search className={styles.filtersBar} aria-label="Filtros del ranking">
			<div className={styles.searchContainer}>
				<div className={styles.searchInputWrapper}>
					<input
						type="search"
						placeholder="Buscar jugador..."
						value={filters.searchTerm}
						onChange={(e) => onFilterChange("searchTerm", e.target.value)}
						className={styles.searchInput}
						aria-label="Buscar jugador por nombre"
					/>
				</div>
			</div>

			<div className={styles.filterControl}>
				<label htmlFor="filter-league-select" className={styles.filterLabel}>
					Liga
				</label>
				<div className={styles.selectWrapper}>
					<select
						id="filter-league-select"
						value={filters.selectedLeague}
						onChange={(e) => onFilterChange("selectedLeague", e.target.value)}
						className={styles.select}
					>
						<option value="">Todas las ligas</option>
						{availableLeagues.map((league) => (
							<option key={league} value={league}>
								{league}
							</option>
						))}
					</select>
				</div>
			</div>

			<div className={styles.filterControl}>
				<label htmlFor="filter-position-select" className={styles.filterLabel}>
					Posición
				</label>
				<div className={styles.selectWrapper}>
					<select
						id="filter-position-select"
						value={filters.selectedPosition}
						onChange={(e) => onFilterChange("selectedPosition", e.target.value)}
						className={styles.select}
					>
						<option value="">Todas las posiciones</option>
						<option value="DEL">Delanteros (DEL)</option>
						<option value="MED">Mediocampistas (MED)</option>
						<option value="DEF">Defensores (DEF)</option>
						<option value="POR">Arqueros (POR)</option>
					</select>
				</div>
			</div>

			<div className={styles.actionsContainer}>
				<button
					type="button"
					onClick={onClearFilters}
					disabled={!hasActiveFilters}
					className={`${styles.clearButton} ${
						hasActiveFilters ? styles.clearButton_active : ""
					}`}
					title="Restablecer todos los filtros"
				>
					<X size={14} aria-hidden="true" />
					<span>Limpiar filtros</span>
				</button>
			</div>
		</search>
	);
}
