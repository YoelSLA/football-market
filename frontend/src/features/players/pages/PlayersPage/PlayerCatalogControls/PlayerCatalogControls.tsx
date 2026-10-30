import { useEffect } from "react";
import type {
	PlayerCatalogCriteria,
	PlayerCatalogFilters,
} from "@/features/players/types";
import { getNationalityFlag } from "@/features/players/utils";
import { Globe, List, Shield, UsersRound, X } from "@/shared/icons";
import { getLeagueShield } from "@/shared/utils/leagueShield";
import { getTeamShield } from "@/shared/utils/teamShield";
import ImageSelect, { type ImageSelectOption } from "./ImageSelect";
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

	const leagueOptions: ImageSelectOption[] = [
		{ value: "", label: "Todas las ligas", image: null },
		...(options?.leagues.map((league) => ({
			value: String(league.id),
			label: league.name,
			image: getLeagueShield(league.name),
		})) ?? []),
	];

	const leagueNameById = new Map(
		options?.leagues.map((league) => [league.id, league.name]) ?? [],
	);
	const teamOptions: ImageSelectOption[] = [
		{ value: "", label: "Todos los equipos", image: null },
		...availableTeams.map((team) => ({
			value: String(team.id),
			label: team.name,
			image: getTeamShield(leagueNameById.get(team.leagueId) ?? "", team.name),
		})),
	];

	const positionOptions: ImageSelectOption[] = [
		{ value: "", label: "Todas las posiciones", image: null },
		...(options?.positions.map((position) => ({
			value: position,
			label: position,
			image: null,
		})) ?? []),
	];

	const nationalityOptions: ImageSelectOption[] = [
		{ value: "", label: "Todas las nacionalidades", image: null },
		...(options?.nationalities.map((nationality) => ({
			value: nationality,
			label: nationality,
			image: getNationalityFlag(nationality),
		})) ?? []),
	];

	const sortOptions: ImageSelectOption[] = [
		{ value: "name,asc", label: "Nombre A–Z", image: null },
		{ value: "name,desc", label: "Nombre Z–A", image: null },
	];

	return (
		<section
			className={styles.playerCatalogControls}
			aria-label="Filtros del catálogo"
		>
			<ImageSelect
				label="Liga"
				icon={<Globe size={18} aria-hidden="true" />}
				value={criteria.leagueId}
				options={leagueOptions}
				disabled={disabled}
				onChange={(value) => onChange("leagueId", value)}
			/>
			<ImageSelect
				label="Equipo"
				icon={<Shield size={18} aria-hidden="true" />}
				value={criteria.teamId}
				options={teamOptions}
				disabled={disabled}
				onChange={(value) => onChange("teamId", value)}
			/>
			<ImageSelect
				label="Posición"
				icon={<UsersRound size={18} aria-hidden="true" />}
				value={criteria.position}
				options={positionOptions}
				disabled={disabled}
				onChange={(value) => onChange("position", value)}
			/>
			<ImageSelect
				label="Nacionalidad"
				icon={<Globe size={18} aria-hidden="true" />}
				value={criteria.nationality}
				options={nationalityOptions}
				disabled={disabled}
				onChange={(value) => onChange("nationality", value)}
			/>
			<ImageSelect
				label="Ordenar por"
				icon={<List size={18} aria-hidden="true" />}
				value={criteria.sort}
				options={sortOptions}
				disabled={false}
				onChange={(value) => onChange("sort", value)}
			/>
			<button type="button" onClick={onClear}>
				<X size={14} aria-hidden="true" />
				Limpiar filtros
			</button>
		</section>
	);
}
