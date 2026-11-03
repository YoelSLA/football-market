import { Button } from "@/shared/components";
import { ShoppingCart } from "@/shared/icons";
import styles from "./MarketFilterFields.module.scss";
import MarketFilterSearch from "./MarketFilterSearch";
import MarketFilterSelect from "./MarketFilterSelect";

interface Props {
	scope: string;
	active?: boolean;
	leagues?: string[];
	positions?: string[];
	search?: string;
	league?: string;
	position?: string;
	availability?: string;
	sort?: string;
	onSearchChange?: (value: string) => void;
	onLeagueChange?: (value: string) => void;
	onPositionChange?: (value: string) => void;
	onAvailabilityChange?: (value: string) => void;
	onSortChange?: (value: string) => void;
	onApply?: () => void;
}

const DIRECTION_OPTIONS = [
	{ value: "asc", label: "Menor a mayor" },
	{ value: "desc", label: "Mayor a menor" },
] as const;

export default function MarketFilterFields({
	scope,
	active,
	leagues,
	positions,
	search,
	league,
	position,
	availability,
	sort,
	onSearchChange,
	onLeagueChange,
	onPositionChange,
	onAvailabilityChange,
	onSortChange,
	onApply,
}: Props) {
	const [sortField = "name", sortDirection = "asc"] = (sort ?? "").split(",");
	const activeField = sortField || "name";
	const activeDirection = sortDirection === "desc" ? "desc" : "asc";

	const leagueOptions = active
		? [
				{ value: "all", label: "Todas las ligas" },
				...(leagues?.map((value) => ({ value, label: value })) ?? []),
			]
		: [{ value: "all", label: "Todas las ligas" }];

	const positionOptions = active
		? [
				{ value: "all", label: "Todas las posiciones" },
				...(positions?.map((value) => ({ value, label: value })) ?? []),
			]
		: [{ value: "all", label: "Todas las posiciones" }];

	const availabilityOptions = [
		{ value: "all", label: "Todas" },
		{ value: "available", label: "Con disponibles" },
		{ value: "reserved", label: "Con reservados" },
	];

	const sortOptions = [
		{ value: "name", label: "Nombre" },
		{ value: "total", label: "Tenencia total" },
		{ value: "available", label: "Tokens disponibles" },
		{ value: "reserved", label: "Tokens reservados" },
	];

	const pendingSortOptions = [{ value: "price", label: "Precio actual" }];
	const pendingPriceOptions = [{ value: "all", label: "Todos los precios" }];

	return (
		<fieldset
			className={styles.marketFilterFields}
			disabled={!active}
			aria-label={active ? undefined : "Filtros pendientes"}
		>
			<MarketFilterSearch
				id={`${scope}-search`}
				className={styles.marketFilterFields__search}
				value={search}
				disabled={!active}
				active={active}
				onChange={onSearchChange}
			/>

			<MarketFilterSelect
				id={`${scope}-league`}
				label="Liga"
				value={active ? (league ?? "all") : undefined}
				defaultValue={active ? undefined : "all"}
				disabled={!active}
				options={leagueOptions}
				onChange={onLeagueChange}
			/>

			<MarketFilterSelect
				id={`${scope}-position`}
				label="Posición"
				value={active ? (position ?? "all") : undefined}
				defaultValue={active ? undefined : "all"}
				disabled={!active}
				options={positionOptions}
				onChange={onPositionChange}
			/>

			{active ? (
				<MarketFilterSelect
					id={`${scope}-availability`}
					label="Disponibilidad"
					value={availability ?? "all"}
					options={availabilityOptions}
					onChange={onAvailabilityChange}
				/>
			) : (
				<MarketFilterSelect
					id={`${scope}-price`}
					label="Precio actual"
					defaultValue="all"
					disabled
					options={pendingPriceOptions}
				/>
			)}

			{active ? (
				<MarketFilterSelect
					id={`${scope}-sort`}
					label="Ordenar por"
					value={activeField}
					options={sortOptions}
					onChange={(newField) => {
						onSortChange?.(`${newField},${activeDirection}`);
					}}
				/>
			) : (
				<MarketFilterSelect
					id={`${scope}-sort-pending`}
					label="Ordenar por"
					defaultValue="price"
					disabled
					options={pendingSortOptions}
				/>
			)}

			<div className={styles.marketFilterFields__direction}>
				<span>Dirección</span>
				{DIRECTION_OPTIONS.map((direction) => (
					<label key={direction.value}>
						<input
							type="radio"
							name={`${scope}-${active ? "direction" : "price-direction"}`}
							value={direction.value}
							checked={
								active
									? activeDirection === direction.value
									: direction.value === "asc"
							}
							disabled={!active}
							onChange={
								active
									? () => onSortChange?.(`${activeField},${direction.value}`)
									: undefined
							}
						/>{" "}
						{direction.label}
					</label>
				))}
			</div>

			<Button
				type="button"
				className={styles.marketFilterFields__apply}
				disabled={!active}
				onClick={active ? onApply : undefined}
			>
				<ShoppingCart aria-hidden="true" /> Aplicar filtros
			</Button>
		</fieldset>
	);
}
