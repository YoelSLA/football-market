import { CATEGORIES, METRIC_DEFINITIONS } from "@/features/players/constants";
import type { PlayerMetric } from "@/features/players/types";
import {
	formatStatisticsValue,
	getPlayerStatisticsCatalog,
} from "@/features/players/utils";
import {
	Activity,
	CircleDot,
	Clock,
	Footprints,
	Goal,
	List,
	Search,
	Shield,
	SlidersHorizontal,
	Star,
} from "@/shared/icons";
import styles from "./PlayerStatisticsCatalog.module.scss";

interface Props {
	metrics: PlayerMetric[];
	position: string;
	search: string;
	category: string;
	unit: string;
	onSearchChange: (text: string) => void;
	onCategoryChange: (category: string) => void;
	onUnitChange: (unit: string) => void;
	onClear: () => void;
}
const categoryIcons = [
	Clock,
	Goal,
	CircleDot,
	Footprints,
	Star,
	Shield,
	Goal,
	Activity,
];
const units = [
	{ key: "PER_MATCH", label: "Por partido" },
	{ key: "SEASON_TOTAL", label: "Total temporada" },
	{ key: "PERCENTAGE", label: "Porcentaje" },
	{ key: "AVERAGE_RATING", label: "Promedio" },
];

export default function PlayerStatisticsCatalog({
	metrics,
	position,
	search,
	category,
	unit,
	onSearchChange,
	onCategoryChange,
	onUnitChange,
	onClear,
}: Props) {
	const rows = getPlayerStatisticsCatalog(
		metrics,
		position,
		search,
		category,
		unit,
	);
	return (
		<section
			className={styles.playerStatisticsCatalog}
			aria-label="Todas las estadísticas"
		>
			<aside
				className={styles.playerStatisticsCatalog__filters}
				aria-label="Filtros de estadísticas"
			>
				<header>
					<h2>
						<SlidersHorizontal size={21} aria-hidden="true" />
						Filtros
					</h2>
					<button type="button" onClick={onClear}>
						Limpiar filtros
					</button>
				</header>
				<label className={styles.playerStatisticsCatalog__search}>
					<Search size={17} aria-hidden="true" />
					<input
						type="search"
						aria-label="Buscar estadística por nombre o clave"
						placeholder="Buscar estadística…"
						value={search}
						onChange={(event) => onSearchChange(event.target.value)}
					/>
				</label>
				<h3>Categoría</h3>
				<button
					type="button"
					aria-pressed={category === "all"}
					onClick={() => onCategoryChange("all")}
				>
					Todas las categorías
				</button>
				<div className={styles.playerStatisticsCatalog__options}>
					{CATEGORIES.map((name, index) => {
						const Icon = categoryIcons[index];
						return (
							<button
								key={name}
								type="button"
								aria-pressed={category === name}
								onClick={() =>
									onCategoryChange(category === name ? "all" : name)
								}
							>
								<span
									className={styles.playerStatisticsCatalog__check}
									aria-hidden="true"
								>
									{category === name ? "✓" : ""}
								</span>
								<Icon size={17} aria-hidden="true" />
								{name}
								<small>
									{
										METRIC_DEFINITIONS.filter((item) => item.category === name)
											.length
									}
								</small>
							</button>
						);
					})}
				</div>
				<h3>Unidad</h3>
				<button
					type="button"
					aria-pressed={unit === "all"}
					onClick={() => onUnitChange("all")}
				>
					Todas las unidades
				</button>
				<div className={styles.playerStatisticsCatalog__options}>
					{units.map((item) => (
						<button
							key={item.key}
							type="button"
							aria-pressed={unit === item.key}
							onClick={() => onUnitChange(unit === item.key ? "all" : item.key)}
						>
							<span
								className={styles.playerStatisticsCatalog__check}
								aria-hidden="true"
							>
								{unit === item.key ? "✓" : ""}
							</span>
							<CircleDot size={16} aria-hidden="true" />
							{item.label}
						</button>
					))}
				</div>
			</aside>
			<div className={styles.playerStatisticsCatalog__panel}>
				<header className={styles.playerStatisticsCatalog__heading}>
					<div>
						<h2>
							<List size={23} aria-hidden="true" />
							Todas las estadísticas
						</h2>
						<p>
							60 métricas canónicas de WhoScored. Valores por partido y totales
							de la temporada.
						</p>
					</div>
					<span role="status">{rows.length} resultados</span>
				</header>
				<table className={styles.playerStatisticsCatalog__table}>
					<caption className={styles.playerStatisticsCatalog__caption}>
						Estadísticas reales, priorizadas según la posición. Cero es un valor
						válido; «Sin datos» indica ausencia.
					</caption>
					<thead>
						<tr>
							<th scope="col">#</th>
							<th scope="col">Estadística</th>
							<th scope="col">Categoría</th>
							<th scope="col">Unidad</th>
							<th scope="col">Valor</th>
						</tr>
					</thead>
					<tbody>
						{rows.map((item, index) => (
							<tr key={item.key}>
								<td>{index + 1}</td>
								<th scope="row">
									<CircleDot size={15} aria-hidden="true" />
									{item.label}
								</th>
								<td>
									<span className={styles.playerStatisticsCatalog__badge}>
										{item.category}
									</span>
								</td>
								<td>{item.unit}</td>
								<td
									className={
										item.value === null
											? styles.playerStatisticsCatalog__absent
											: undefined
									}
								>
									{formatStatisticsValue(item.value)}
									{item.unit === "%" && item.value !== null ? " %" : ""}
								</td>
							</tr>
						))}
					</tbody>
				</table>
				{rows.length === 0 && (
					<p role="status">No se encontraron estadísticas con esos filtros.</p>
				)}
			</div>
		</section>
	);
}
