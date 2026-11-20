import type {
	PlayerCatalogCriteria,
	PlayerCatalogFilters,
} from "@/features/players/types";
import { Button, FilterSearch } from "@/shared/components";
import PlayerCatalogControls from "../PlayerCatalogControls";
import styles from "./PlayersToolbar.module.scss";

interface Props {
	totalElements?: number;
	searchText: string;
	onSearchChange: (value: string) => void;
	criteria: PlayerCatalogCriteria;
	options: PlayerCatalogFilters | undefined;
	optionsLoading: boolean;
	optionsError: string | null;
	onChangeFilter: (
		key: "leagueId" | "teamId" | "position" | "nationality" | "sort",
		value: string,
	) => void;
	onClearFilters: () => void;
	onRetryOptions: () => void;
}

export default function PlayersToolbar({
	totalElements,
	searchText,
	onSearchChange,
	criteria,
	options,
	optionsLoading,
	optionsError,
	onChangeFilter,
	onClearFilters,
	onRetryOptions,
}: Props) {
	return (
		<div className={styles.playersToolbar}>
			<div className={styles.playersToolbar__heading}>
				<h1>Catálogo de jugadores</h1>
				{totalElements !== undefined && (
					<span>{totalElements} jugadores activos</span>
				)}
				<FilterSearch
					id="player-catalog-search"
					className={styles.playersToolbar__search}
					label="Buscar jugador"
					accessibleLabel="Buscar jugador por nombre"
					placeholder="Buscar jugador…"
					value={searchText}
					onChange={onSearchChange}
					maxLength={255}
				/>
			</div>

			<PlayerCatalogControls
				criteria={criteria}
				options={options}
				disabled={optionsLoading || !options}
				onChange={onChangeFilter}
				onClear={onClearFilters}
			/>

			{optionsError && (
				<div role="alert" className={styles.playersToolbar__optionsError}>
					{optionsError}{" "}
					<Button type="button" onClick={onRetryOptions}>
						Reintentar filtros
					</Button>
				</div>
			)}
		</div>
	);
}
