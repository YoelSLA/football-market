import type { RankingFilterCriteria } from "@/features/players/types";
import { Button } from "@/shared/components";
import { X } from "@/shared/icons";
import styles from "./RankingsFilters.module.scss";

interface Props {
	filters: RankingFilterCriteria;
	onFilterChange: (key: keyof RankingFilterCriteria, value: string) => void;
	onClearFilters: () => void;
	availableLeagues: { id: number; name: string }[];
	availablePositions: string[];
}

export default function RankingsFilters({
	filters,
	onFilterChange,
	onClearFilters,
	availableLeagues,
	availablePositions,
}: Props) {
	const hasActiveFilters =
		Boolean(filters.name.trim()) ||
		Boolean(filters.leagueId) ||
		Boolean(filters.position);

	return (
		<search className={styles.filtersBar} aria-label="Filtros del ranking">
			<div className={styles.searchContainer}>
				<div className={styles.searchInputWrapper}>
					<input
						type="search"
						placeholder="Buscar jugador..."
						value={filters.name}
						onChange={(e) => onFilterChange("name", e.target.value)}
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
						value={filters.leagueId}
						onChange={(e) => onFilterChange("leagueId", e.target.value)}
						className={styles.select}
					>
						<option value="">Todas las ligas</option>
						{availableLeagues.map((league) => (
							<option key={league.id} value={String(league.id)}>
								{league.name}
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
						value={filters.position}
						onChange={(e) => onFilterChange("position", e.target.value)}
						className={styles.select}
					>
						<option value="">Todas las posiciones</option>
						{availablePositions.map((position) => (
							<option key={position} value={position}>
								{position}
							</option>
						))}
					</select>
				</div>
			</div>

			<div className={styles.actionsContainer}>
				<Button
					unstyled
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
				</Button>
			</div>
		</search>
	);
}
