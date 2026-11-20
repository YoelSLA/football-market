import {
	type PlayerCatalogFilters,
	sanitizePositions,
} from "@/features/players";
import {
	CompetitionLogo,
	FilterSearch,
	FilterSelect,
	type FilterSelectOption,
	TeamBadge,
} from "@/shared/components";
import styles from "./MarketFilterFields.module.scss";

interface Props {
	scope: string;
	active: boolean;
	options?: PlayerCatalogFilters;
	leagues?: string[];
	positions?: string[];
	search?: string;
	league?: string;
	team?: string;
	position?: string;
	availability?: string;
	sort?: string;
	onSearchChange?: (value: string) => void;
	onLeagueChange?: (value: string) => void;
	onTeamChange?: (value: string) => void;
	onPositionChange?: (value: string) => void;
	onAvailabilityChange?: (value: string) => void;
	onSortChange?: (value: string) => void;
	onApply?: () => void;
}

const AVAILABILITY_OPTIONS = [
	{ value: "all", label: "Todas las tenencias" },
	{ value: "available", label: "Con disponibles" },
	{ value: "reserved", label: "Con reservados" },
];

const SORT_OPTIONS = [
	{ value: "name,asc", label: "Nombre (A–Z)" },
	{ value: "name,desc", label: "Nombre (Z–A)" },
	{ value: "total,desc", label: "Mayor tenencia total" },
	{ value: "total,asc", label: "Menor tenencia total" },
	{ value: "available,desc", label: "Más tokens disponibles" },
	{ value: "available,asc", label: "Menos tokens disponibles" },
	{ value: "reserved,desc", label: "Más tokens reservados" },
	{ value: "reserved,asc", label: "Menos tokens reservados" },
];

export default function SellMarketFilterFields({
	scope,
	active,
	options,
	search,
	league = "all",
	team = "all",
	position = "all",
	availability = "all",
	sort,
	onSearchChange,
	onLeagueChange,
	onTeamChange,
	onPositionChange,
	onAvailabilityChange,
	onSortChange,
}: Props) {
	const teams = options?.teams.filter(
		(option) => league === "all" || String(option.leagueId) === league,
	);
	const leagueNameById = new Map(
		options?.leagues.map((option) => [option.id, option.name]) ?? [],
	);

	const leagueOptions: FilterSelectOption[] = [
		{ value: "all", label: "Todas" },
		...(options?.leagues.map((option) => ({
			value: String(option.id),
			label: option.name,
			visual: (
				<CompetitionLogo
					competitionId={option.id}
					competitionName={option.name}
					decorative
				/>
			),
		})) ?? []),
	];

	const teamOptions: FilterSelectOption[] = [
		{ value: "all", label: "Todos" },
		...(teams?.map((option) => ({
			value: String(option.id),
			label: option.name,
			visual: (
				<TeamBadge
					teamId={option.id}
					teamName={option.name}
					leagueId={option.leagueId}
					leagueName={leagueNameById.get(option.leagueId)}
				/>
			),
		})) ?? []),
	];

	const positionOptions: FilterSelectOption[] = [
		{ value: "all", label: "Todas" },
		...sanitizePositions(options?.positions).map((value) => ({
			value,
			label: value,
		})),
	];

	return (
		<fieldset
			className={`${styles.marketFilterFields} ${styles.marketFilterFields_sell}`}
			disabled={!active}
			aria-label={active ? undefined : "Filtros pendientes"}
		>
			<FilterSearch
				id={`${scope}-search`}
				className={styles.marketFilterFields__search}
				value={search ?? ""}
				disabled={!active}
				label="Buscar jugador o club"
				accessibleLabel="Buscar jugador por nombre o club"
				placeholder="Buscar jugador o club..."
				onChange={(value) => onSearchChange?.(value)}
			/>
			<FilterSelect
				id={`${scope}-league`}
				label="Liga"
				value={league}
				disabled={!active}
				options={leagueOptions}
				searchable
				searchPlaceholder="Buscar liga…"
				onChange={(value) => onLeagueChange?.(value)}
			/>
			<FilterSelect
				id={`${scope}-team`}
				label="Equipo"
				value={team}
				disabled={!active}
				options={teamOptions}
				searchable
				searchPlaceholder="Buscar equipo…"
				onChange={(value) => onTeamChange?.(value)}
			/>
			<FilterSelect
				id={`${scope}-position`}
				label="Posición"
				value={position}
				disabled={!active}
				options={positionOptions}
				onChange={(value) => onPositionChange?.(value)}
			/>
			<FilterSelect
				id={`${scope}-availability`}
				label="Disponibilidad"
				value={availability}
				disabled={!active}
				options={AVAILABILITY_OPTIONS}
				onChange={(value) => onAvailabilityChange?.(value)}
			/>
			<FilterSelect
				id={`${scope}-sort`}
				label="Ordenar por"
				value={sort || "name,asc"}
				disabled={!active}
				options={SORT_OPTIONS}
				onChange={(value) => onSortChange?.(value)}
			/>
		</fieldset>
	);
}
