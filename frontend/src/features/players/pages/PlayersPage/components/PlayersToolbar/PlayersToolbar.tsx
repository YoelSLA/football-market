import { useEffect } from "react";
import type {
	PlayerCatalogCriteria,
	PlayerCatalogFilters,
} from "@/features/players/types";
import { sanitizePositions } from "@/features/players/utils";
import {
	CompetitionLogo,
	FilterPopover,
	FilterSearch,
	FilterSelect,
	type FilterSelectOption,
	NationalityFlag,
	TeamBadge,
} from "@/shared/components";
import { X } from "@/shared/icons";
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
	const availableTeams = options
		? criteria.leagueId
			? options.teams.filter(
					(team) => String(team.leagueId) === criteria.leagueId,
				)
			: options.teams
		: [];

	useEffect(() => {
		if (!options || !criteria.teamId) return;
		const stillValid = availableTeams.some(
			(team) => String(team.id) === criteria.teamId,
		);
		if (!stillValid) onChangeFilter("teamId", "");
	}, [criteria.teamId, options, availableTeams, onChangeFilter]);

	const leagueOptions: FilterSelectOption[] = [
		{ value: "", label: "Todas las ligas" },
		...(options?.leagues.map((league) => ({
			value: String(league.id),
			label: league.name,
			visual: (
				<CompetitionLogo
					competitionId={league.id}
					competitionName={league.name}
					decorative
				/>
			),
		})) ?? []),
	];

	const leagueNameById = new Map(
		options?.leagues.map((league) => [league.id, league.name]) ?? [],
	);

	const teamOptions: FilterSelectOption[] = [
		{ value: "", label: "Todos los equipos" },
		...availableTeams.map((team) => ({
			value: String(team.id),
			label: team.name,
			visual: (
				<TeamBadge
					teamId={team.id}
					teamName={team.name}
					leagueId={team.leagueId}
					leagueName={leagueNameById.get(team.leagueId)}
				/>
			),
		})),
	];

	const positionOptions: FilterSelectOption[] = [
		{ value: "", label: "Todas las posiciones" },
		...sanitizePositions(options?.positions).map((position) => ({
			value: position,
			label: position,
		})),
	];

	const nationalityOptions: FilterSelectOption[] = [
		{ value: "", label: "Todas las nacionalidades" },
		...(options?.nationalities.map((nationality) => ({
			value: nationality,
			label: nationality,
			visual: <NationalityFlag nationality={nationality} />,
		})) ?? []),
	];

	const sortOptions: FilterSelectOption[] = [
		{ value: "name,asc", label: "Nombre A–Z" },
		{ value: "name,desc", label: "Nombre Z–A" },
	];

	const popoverActiveCount = [
		Boolean(criteria.teamId),
		Boolean(criteria.nationality),
		Boolean(criteria.sort && criteria.sort !== "name,asc"),
	].filter(Boolean).length;

	const hasActiveFilters = Boolean(
		searchText.trim() ||
			criteria.leagueId ||
			criteria.teamId ||
			criteria.position ||
			criteria.nationality ||
			(criteria.sort && criteria.sort !== "name,asc"),
	);

	const handleClearPopoverFilters = () => {
		onChangeFilter("teamId", "");
		onChangeFilter("nationality", "");
		onChangeFilter("sort", "name,asc");
	};

	const controlsDisabled = optionsLoading || !options;

	return (
		<search aria-label="Filtros del catálogo" className={styles.toolbar}>
			<div className={styles.toolbar__left}>
				<FilterSearch
					id="player-catalog-search"
					className={styles.toolbar__search}
					label="Buscar jugador"
					accessibleLabel="Buscar jugador por nombre"
					placeholder="Buscar jugador…"
					value={searchText}
					onChange={onSearchChange}
					maxLength={255}
				/>
				<FilterSelect
					id="player-catalog-league"
					className={styles.toolbar__select}
					label="Liga"
					value={criteria.leagueId}
					options={leagueOptions}
					disabled={controlsDisabled}
					searchable
					searchPlaceholder="Buscar liga…"
					onChange={(value) => onChangeFilter("leagueId", value)}
				/>
				<FilterSelect
					id="player-catalog-position"
					className={styles.toolbar__select}
					label="Posición"
					value={criteria.position}
					options={positionOptions}
					disabled={controlsDisabled}
					onChange={(value) => onChangeFilter("position", value)}
				/>
				<FilterPopover
					triggerLabel="Más filtros"
					title="Filtros avanzados"
					activeCount={popoverActiveCount}
					onClear={handleClearPopoverFilters}
					panelWidth="md"
					disabled={controlsDisabled}
				>
					<FilterSelect
						id="player-catalog-team"
						label="Equipo"
						value={criteria.teamId}
						options={teamOptions}
						disabled={controlsDisabled}
						searchable
						searchPlaceholder="Buscar equipo…"
						onChange={(value) => onChangeFilter("teamId", value)}
					/>
					<FilterSelect
						id="player-catalog-nationality"
						label="Nacionalidad"
						value={criteria.nationality}
						options={nationalityOptions}
						disabled={controlsDisabled}
						searchable
						searchPlaceholder="Buscar nacionalidad…"
						onChange={(value) => onChangeFilter("nationality", value)}
					/>
					<FilterSelect
						id="player-catalog-sort"
						label="Ordenar por"
						value={criteria.sort}
						options={sortOptions}
						disabled={false}
						onChange={(value) => onChangeFilter("sort", value)}
					/>
				</FilterPopover>

				{optionsError && (
					<div role="alert" className={styles.toolbar__error}>
						<span>{optionsError}</span>
						<button
							type="button"
							className={styles.toolbar__retry}
							onClick={onRetryOptions}
						>
							Reintentar
						</button>
					</div>
				)}
			</div>

			{hasActiveFilters && (
				<div className={styles.toolbar__right}>
					<button
						type="button"
						className={styles.toolbar__clear}
						onClick={onClearFilters}
						title="Restablecer todos los filtros"
					>
						<X aria-hidden="true" />
						<span>Limpiar</span>
					</button>
				</div>
			)}
		</search>
	);
}
