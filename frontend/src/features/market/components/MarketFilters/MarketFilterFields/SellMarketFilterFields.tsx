import { sanitizePositions } from "@/features/players";
import { Button, FilterSearch, FilterSelect } from "@/shared/components";
import { ShoppingCart } from "@/shared/icons";
import styles from "./MarketFilterFields.module.scss";

interface Props {
	scope: string;
	active: boolean;
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

export default function SellMarketFilterFields({
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
				...sanitizePositions(positions).map((value) => ({
					value,
					label: value,
				})),
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
				placeholder="Buscar jugador o club..."
				onChange={(value) => onSearchChange?.(value)}
			/>
			<FilterSelect
				id={`${scope}-league`}
				label="Liga"
				value={league ?? "all"}
				disabled={!active}
				options={leagueOptions}
				onChange={(value) => onLeagueChange?.(value)}
			/>
			<FilterSelect
				id={`${scope}-position`}
				label="Posición"
				value={position ?? "all"}
				disabled={!active}
				options={positionOptions}
				onChange={(value) => onPositionChange?.(value)}
			/>
			<FilterSelect
				id={`${scope}-${active ? "availability" : "price"}`}
				label={active ? "Disponibilidad" : "Precio actual"}
				value={availability ?? "all"}
				disabled={!active}
				options={
					active
						? availabilityOptions
						: [{ value: "all", label: "Todos los precios" }]
				}
				onChange={(value) => onAvailabilityChange?.(value)}
			/>
			<FilterSelect
				id={`${scope}-${active ? "sort" : "sort-pending"}`}
				label="Ordenar por"
				value={active ? activeField : "price"}
				disabled={!active}
				options={
					active ? sortOptions : [{ value: "price", label: "Precio actual" }]
				}
				onChange={(newField) =>
					onSortChange?.(`${newField},${activeDirection}`)
				}
			/>
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
