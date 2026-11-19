import { useEffect } from "react";
import type {
	PlayerCatalogCriteria,
	PlayerCatalogFilters,
} from "@/features/players/types";
import { sanitizePositions } from "@/features/players/utils";
import {
	Button,
	CompetitionLogo,
	FilterSelect,
	type FilterSelectOption,
	NationalityFlag,
	TeamBadge,
} from "@/shared/components";
import styles from "./PlayerCatalogControls.module.scss";

interface Props {
	criteria: PlayerCatalogCriteria;
	options: PlayerCatalogFilters | undefined;
	disabled: boolean;
	onChange: (
		key: "leagueId" | "teamId" | "position" | "nationality" | "sort",
		value: string,
	) => void;
	onClear: () => void;
}

export default function PlayerCatalogControls({
	criteria,
	options,
	disabled,
	onChange,
	onClear,
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
		if (!stillValid) onChange("teamId", "");
	}, [criteria.teamId, options, availableTeams, onChange]);

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

	return (
		<section
			className={styles.playerCatalogControls}
			aria-label="Filtros del catálogo"
		>
			<FilterSelect
				id="player-catalog-league"
				label="Liga"
				value={criteria.leagueId}
				options={leagueOptions}
				disabled={disabled}
				searchable
				searchPlaceholder="Buscar liga…"
				onChange={(value) => onChange("leagueId", value)}
			/>
			<FilterSelect
				id="player-catalog-team"
				label="Equipo"
				value={criteria.teamId}
				options={teamOptions}
				disabled={disabled}
				searchable
				searchPlaceholder="Buscar equipo…"
				onChange={(value) => onChange("teamId", value)}
			/>
			<FilterSelect
				id="player-catalog-position"
				label="Posición"
				value={criteria.position}
				options={positionOptions}
				disabled={disabled}
				onChange={(value) => onChange("position", value)}
			/>
			<FilterSelect
				id="player-catalog-nationality"
				label="Nacionalidad"
				value={criteria.nationality}
				options={nationalityOptions}
				disabled={disabled}
				searchable
				searchPlaceholder="Buscar nacionalidad…"
				onChange={(value) => onChange("nationality", value)}
			/>
			<FilterSelect
				id="player-catalog-sort"
				label="Ordenar por"
				value={criteria.sort}
				options={sortOptions}
				disabled={false}
				onChange={(value) => onChange("sort", value)}
			/>
			<Button
				unstyled
				type="button"
				className={styles.playerCatalogControls__clear}
				onClick={onClear}
			>
				Limpiar filtros
			</Button>
		</section>
	);
}
