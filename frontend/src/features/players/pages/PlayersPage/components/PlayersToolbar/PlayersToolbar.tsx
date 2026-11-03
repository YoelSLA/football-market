import type {
	PlayerCatalogCriteria,
	PlayerCatalogFilters,
} from "@/features/players/types";
import { Button } from "@/shared/components";
import { Search } from "@/shared/icons";
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
				<label className={styles.playersToolbar__search}>
					<Search size={18} aria-hidden="true" />
					<input
						type="search"
						aria-label="Buscar jugador por nombre"
						placeholder="Buscar jugador…"
						value={searchText}
						onChange={(event) => onSearchChange(event.target.value)}
						maxLength={255}
					/>
				</label>
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
