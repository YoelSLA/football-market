import { formatCredits } from "@/features/market/utils";
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
	NationalityFlag,
	Slider,
	TeamBadge,
} from "@/shared/components";
import styles from "./MarketFilterFields.module.scss";

interface Props {
	scope: string;
	options?: PlayerCatalogFilters;
	search: string;
	league: string;
	team: string;
	position: string;
	nationality: string;
	sort: string;
	priceRange: { min: number | null; max: number | null };
	selectedPriceRange: readonly [number, number] | null;
	onSearchChange: (value: string) => void;
	onLeagueChange: (value: string) => void;
	onTeamChange: (value: string) => void;
	onPositionChange: (value: string) => void;
	onNationalityChange: (value: string) => void;
	onSortChange: (value: string) => void;
	onPriceRangeCommit: (value: readonly [number, number]) => void;
}

const SORT_OPTIONS = [
	{ value: "id,asc", label: "Identificador ascendente" },
	{ value: "id,desc", label: "Identificador descendente" },
	{ value: "name,asc", label: "Nombre A–Z" },
	{ value: "name,desc", label: "Nombre Z–A" },
	{ value: "price,asc", label: "Menor precio" },
	{ value: "price,desc", label: "Mayor precio" },
	{ value: "variation,asc", label: "Menor variación" },
	{ value: "variation,desc", label: "Mayor variación" },
];

export default function OfficialMarketFilterFields({
	scope,
	options,
	search,
	league,
	team,
	position,
	nationality,
	sort,
	priceRange,
	selectedPriceRange,
	onSearchChange,
	onLeagueChange,
	onTeamChange,
	onPositionChange,
	onNationalityChange,
	onSortChange,
	onPriceRangeCommit,
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
	const nationalityOptions: FilterSelectOption[] = [
		{ value: "all", label: "Todas las nacionalidades" },
		...(options?.nationalities.map((value) => ({
			value,
			label: value,
			visual: <NationalityFlag nationality={value} />,
		})) ?? []),
	];

	const isPriceRangeActive = Boolean(
		selectedPriceRange &&
			priceRange.min !== null &&
			priceRange.max !== null &&
			(selectedPriceRange[0] > priceRange.min ||
				selectedPriceRange[1] < priceRange.max),
	);

	const popoverActiveCount = [
		team !== "all",
		nationality !== "all",
		isPriceRangeActive,
		sort !== "id,asc",
	].filter(Boolean).length;

	const handleClearPopover = () => {
		onTeamChange("all");
		onNationalityChange("all");
		if (priceRange.min !== null && priceRange.max !== null) {
			onPriceRangeCommit([priceRange.min, priceRange.max]);
		}
		onSortChange("id,asc");
	};

	return (
		<div className={styles.fields}>
			<FilterSearch
				id={`${scope}-search`}
				className={styles.search}
				value={search}
				onChange={onSearchChange}
				label="Buscar jugador"
				accessibleLabel="Buscar jugador por nombre o apellido"
				placeholder="Buscar jugador…"
			/>
			<FilterSelect
				id={`${scope}-league`}
				className={styles.select}
				label="Liga"
				value={league}
				options={leagueOptions}
				searchable
				searchPlaceholder="Buscar liga…"
				onChange={onLeagueChange}
			/>
			<FilterSelect
				id={`${scope}-position`}
				className={styles.select}
				label="Posición"
				value={position}
				options={[
					{ value: "all", label: "Todas las posiciones" },
					...sanitizePositions(options?.positions).map((value) => ({
						value,
						label: value,
					})),
				]}
				onChange={onPositionChange}
			/>
			<FilterPopover
				triggerLabel="Más filtros"
				title="Filtros avanzados"
				activeCount={popoverActiveCount}
				onClear={handleClearPopover}
				panelWidth="md"
			>
				<FilterSelect
					id={`${scope}-team`}
					className={styles.popoverField}
					label="Equipo"
					value={team}
					options={teamOptions}
					searchable
					searchPlaceholder="Buscar equipo…"
					onChange={onTeamChange}
				/>
				<FilterSelect
					id={`${scope}-nationality`}
					className={styles.popoverField}
					label="Nacionalidad"
					value={nationality}
					options={nationalityOptions}
					searchable
					searchPlaceholder="Buscar nacionalidad…"
					onChange={onNationalityChange}
				/>
				{priceRange.min !== null &&
				priceRange.max !== null &&
				selectedPriceRange ? (
					<div className={styles.popoverSlider}>
						<Slider
							id={`${scope}-price-range`}
							variant="range"
							label="Rango de precio"
							min={priceRange.min}
							max={priceRange.max}
							step={1}
							value={selectedPriceRange}
							formatValue={formatCredits}
							disabled={priceRange.min === priceRange.max}
							onCommit={onPriceRangeCommit}
						/>
					</div>
				) : (
					<div className={styles.popoverStatus} role="status">
						Rango de precio: sin cotizaciones disponibles
					</div>
				)}
				<FilterSelect
					id={`${scope}-sort`}
					className={styles.popoverField}
					label="Orden"
					value={sort}
					options={SORT_OPTIONS}
					onChange={onSortChange}
				/>
			</FilterPopover>
		</div>
	);
}
