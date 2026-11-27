import type { RankingFilterCriteria } from "@/features/players/types";
import { sanitizePositions } from "@/features/players/utils";
import {
	Button,
	CompetitionLogo,
	FilterSearch,
	FilterSelect,
	type FilterSelectOption,
} from "@/shared/components";
import { X } from "@/shared/icons";
import styles from "./RankingsFilters.module.scss";

interface Props {
	filters: RankingFilterCriteria;
	onFilterChange: (key: keyof RankingFilterCriteria, value: string) => void;
	onClearFilters: () => void;
	availableLeagues: { id: number; name: string }[];
	availablePositions: string[];
}

const SORT_OPTIONS: FilterSelectOption[] = [
	{ value: "score_desc", label: "Ordenar: Mayor score" },
	{ value: "score_asc", label: "Ordenar: Menor score" },
	{ value: "price_desc", label: "Ordenar: Mayor cotización" },
	{ value: "price_asc", label: "Ordenar: Menor cotización" },
];

export default function RankingsFilters({
	filters,
	onFilterChange,
	onClearFilters,
	availableLeagues,
	availablePositions,
}: Props) {
	const leagueOptions: FilterSelectOption[] = [
		{ value: "ALL", label: "Todas las ligas" },
		...availableLeagues.map((league) => ({
			value: String(league.id),
			label: league.name,
			visual: (
				<CompetitionLogo
					competitionId={league.id}
					competitionName={league.name}
					decorative
				/>
			),
		})),
	];

	const positionOptions: FilterSelectOption[] = [
		{ value: "ALL", label: "Todas las posiciones" },
		...sanitizePositions(availablePositions).map((p) => ({
			value: p,
			label: p,
		})),
	];

	const hasActiveFilters =
		Boolean(filters.name.trim()) ||
		(Boolean(filters.leagueId) && filters.leagueId !== "ALL") ||
		(Boolean(filters.position) && filters.position !== "ALL") ||
		Boolean(filters.sort && filters.sort !== "score_desc");

	return (
		<search className={styles.toolbar} aria-label="Filtros del ranking">
			<div className={styles.toolbar__left}>
				<FilterSearch
					id="ranking-filter-search"
					className={styles.toolbar__search}
					label="Buscar jugador"
					accessibleLabel="Buscar jugador por nombre"
					placeholder="Buscar jugador…"
					value={filters.name}
					onChange={(val) => onFilterChange("name", val)}
				/>

				<FilterSelect
					id="ranking-filter-league"
					className={styles.toolbar__select}
					label="Liga"
					value={filters.leagueId || "ALL"}
					options={leagueOptions}
					searchable
					searchPlaceholder="Buscar liga…"
					onChange={(val) =>
						onFilterChange("leagueId", val === "ALL" ? "" : val)
					}
				/>

				<FilterSelect
					id="ranking-filter-position"
					className={styles.toolbar__select}
					label="Posición"
					value={filters.position || "ALL"}
					options={positionOptions}
					onChange={(val) =>
						onFilterChange("position", val === "ALL" ? "" : val)
					}
				/>

				<FilterSelect
					id="ranking-filter-sort"
					className={styles.toolbar__sort}
					label="Ordenar"
					value={filters.sort || "score_desc"}
					options={SORT_OPTIONS}
					onChange={(val) => onFilterChange("sort", val)}
				/>
			</div>

			<div className={styles.toolbar__right}>
				<Button
					unstyled
					type="button"
					onClick={onClearFilters}
					disabled={!hasActiveFilters}
					className={`${styles.toolbar__clearBtn} ${
						hasActiveFilters ? styles.toolbar__clearBtn_active : ""
					}`}
					title="Restablecer filtros"
				>
					<X size={14} aria-hidden="true" />
					<span>Limpiar</span>
				</Button>
			</div>
		</search>
	);
}
