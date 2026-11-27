import {
	type PlayerCatalogFilters,
	sanitizePositions,
} from "@/features/players";
import {
	CompetitionLogo,
	FilterPopover,
	FilterSearch,
	FilterSelect,
	type FilterSelectOption,
	TeamBadge,
} from "@/shared/components";
import { TOKEN_SYMBOL } from "@/shared/constants";
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
	{ value: "available,desc", label: `Más ${TOKEN_SYMBOL} disponibles` },
	{ value: "available,asc", label: `Menos ${TOKEN_SYMBOL} disponibles` },
	{ value: "reserved,desc", label: `Más ${TOKEN_SYMBOL} reservados` },
	{ value: "reserved,asc", label: `Menos ${TOKEN_SYMBOL} reservados` },
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
		{ value: "all", label: "Todas las ligas" },
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
		{ value: "all", label: "Todos los equipos" },
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
		{ value: "all", label: "Todas las posiciones" },
		...sanitizePositions(options?.positions).map((value) => ({
			value,
			label: value,
		})),
	];

	const popoverActiveCount = [
		team !== "all",
		availability !== "all",
		Boolean(sort && sort !== "name,asc"),
	].filter(Boolean).length;

	const handleClearPopover = () => {
		onTeamChange?.("all");
		onAvailabilityChange?.("all");
		onSortChange?.("name,asc");
	};

	return (
		<div className={styles.fields}>
			<FilterSearch
				id={`${scope}-search`}
				className={styles.search}
				value={search ?? ""}
				disabled={!active}
				label="Buscar jugador o club"
				accessibleLabel="Buscar jugador por nombre o club"
				placeholder="Buscar jugador o club…"
				onChange={(value) => onSearchChange?.(value)}
			/>
			<FilterSelect
				id={`${scope}-league`}
				className={styles.select}
				label="Liga"
				value={league}
				disabled={!active}
				options={leagueOptions}
				searchable
				searchPlaceholder="Buscar liga…"
				onChange={(value) => onLeagueChange?.(value)}
			/>
			<FilterSelect
				id={`${scope}-position`}
				className={styles.select}
				label="Posición"
				value={position}
				disabled={!active}
				options={positionOptions}
				onChange={(value) => onPositionChange?.(value)}
			/>
			<FilterPopover
				triggerLabel="Más filtros"
				title="Filtros avanzados"
				activeCount={popoverActiveCount}
				onClear={handleClearPopover}
				panelWidth="md"
				disabled={!active}
			>
				<FilterSelect
					id={`${scope}-team`}
					className={styles.popoverField}
					label="Equipo"
					value={team}
					disabled={!active}
					options={teamOptions}
					searchable
					searchPlaceholder="Buscar equipo…"
					onChange={(value) => onTeamChange?.(value)}
				/>
				<FilterSelect
					id={`${scope}-availability`}
					className={styles.popoverField}
					label="Disponibilidad"
					value={availability}
					disabled={!active}
					options={AVAILABILITY_OPTIONS}
					onChange={(value) => onAvailabilityChange?.(value)}
				/>
				<FilterSelect
					id={`${scope}-sort`}
					className={styles.popoverField}
					label="Ordenar por"
					value={sort || "name,asc"}
					disabled={!active}
					options={SORT_OPTIONS}
					onChange={(value) => onSortChange?.(value)}
				/>
			</FilterPopover>
		</div>
	);
}
