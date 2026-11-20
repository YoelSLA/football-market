import { sanitizePositions } from "@/features/players";
import { FilterSearch, FilterSelect } from "@/shared/components";
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
}: Props) {
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
				id={`${scope}-availability`}
				label="Disponibilidad"
				value={availability ?? "all"}
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
