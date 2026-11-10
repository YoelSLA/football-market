import { CATEGORIES } from "@/features/players/constants";
import type { PlayerMetric } from "@/features/players/types";
import {
	formatStatisticsValue,
	getCategoryCounts,
	getPlayerStatisticsCatalog,
	getPlayerStatisticsCatalogGroups,
	getStatisticsPositionLabel,
} from "@/features/players/utils";
import { Button } from "@/shared/components";
import { List, Search } from "@/shared/icons";
import styles from "./PlayerStatisticsCatalog.module.scss";

interface Props {
	metrics: PlayerMetric[];
	position: string;
	search: string;
	category: string;
	unit?: string;
	onSearchChange: (text: string) => void;
	onCategoryChange: (category: string) => void;
	onUnitChange?: (unit: string) => void;
	onClear: () => void;
}

export default function PlayerStatisticsCatalog({
	metrics,
	position,
	search,
	category,
	onSearchChange,
	onCategoryChange,
	onClear,
}: Props) {
	const positionLabel = getStatisticsPositionLabel(position);
	const categoryCounts = getCategoryCounts();
	const catalogMetrics = getPlayerStatisticsCatalog(
		metrics,
		position,
		search,
		category,
	);
	const groups = getPlayerStatisticsCatalogGroups(catalogMetrics);
	const hasActiveFilters = Boolean(search || category !== "all");

	return (
		<section
			className={styles.playerStatisticsCatalog}
			aria-label="Catálogo completo de estadísticas"
		>
			<header className={styles.playerStatisticsCatalog__header}>
				<div>
					<h2>
						<List size={20} aria-hidden="true" />
						Todas las estadísticas
					</h2>
					<p>
						Ordenadas para: <strong>{positionLabel.toUpperCase()}</strong>. La
						posición determina relevancia y orden, pero nunca oculta métricas.
					</p>
				</div>
				<span
					className={styles.playerStatisticsCatalog__headerBadge}
					role="status"
					aria-label={`${catalogMetrics.length} métricas disponibles`}
				>
					{catalogMetrics.length} de 60 métricas
				</span>
			</header>

			<div className={styles.playerStatisticsCatalog__controls}>
				<div className={styles.playerStatisticsCatalog__searchRow}>
					<label className={styles.playerStatisticsCatalog__searchLabel}>
						<Search size={16} aria-hidden="true" />
						<input
							type="search"
							aria-label="Buscar estadística por nombre o clave"
							placeholder="Buscar por nombre o clave…"
							value={search}
							onChange={(event) => onSearchChange(event.target.value)}
						/>
					</label>
					{hasActiveFilters && (
						<Button
							type="button"
							onClick={onClear}
							className={styles.playerStatisticsCatalog__clearBtn}
						>
							Limpiar
						</Button>
					)}
				</div>

				<div
					className={styles.playerStatisticsCatalog__categories}
					role="toolbar"
					aria-label="Filtro por categoría"
				>
					<button
						type="button"
						aria-pressed={category === "all"}
						className={styles.playerStatisticsCatalog__categoryBtn}
						onClick={() => onCategoryChange("all")}
					>
						<span>Todas</span>
						<small>{categoryCounts.all ?? 60}</small>
					</button>
					{CATEGORIES.map((cat) => {
						const count = categoryCounts[cat] ?? 0;
						const isPressed = category === cat;

						return (
							<button
								key={cat}
								type="button"
								aria-pressed={isPressed}
								className={styles.playerStatisticsCatalog__categoryBtn}
								onClick={() => onCategoryChange(isPressed ? "all" : cat)}
							>
								<span>{cat}</span>
								<small>{count}</small>
							</button>
						);
					})}
				</div>

				<div
					className={styles.playerStatisticsCatalog__legend}
					role="note"
					aria-label="Leyenda de relevancia de métricas"
				>
					<span className={styles.playerStatisticsCatalog__legendLabel}>
						Relevancia:
					</span>
					<span className={styles.playerStatisticsCatalog__legendItem}>
						<span
							className={`${styles.dot} ${styles.dotPrincipal}`}
							aria-hidden="true"
						>
							●
						</span>
						<span>Principal</span>
					</span>
					<span className={styles.playerStatisticsCatalog__legendItem}>
						<span
							className={`${styles.dot} ${styles.dotSecundaria}`}
							aria-hidden="true"
						>
							○
						</span>
						<span>Secundaria</span>
					</span>
					<span className={styles.playerStatisticsCatalog__legendItem}>
						<span
							className={`${styles.dot} ${styles.dotPocoRelevante}`}
							aria-hidden="true"
						>
							·
						</span>
						<span>Poco relevante</span>
					</span>
				</div>
			</div>

			{groups.length === 0 ? (
				<div className={styles.playerStatisticsCatalog__empty} role="status">
					<p>
						No se encontraron estadísticas que coincidan con los filtros
						aplicados.
					</p>
					<Button type="button" onClick={onClear}>
						Limpiar filtros
					</Button>
				</div>
			) : (
				<div className={styles.playerStatisticsCatalog__groups}>
					{groups.map((group) => (
						<section
							key={group.category}
							className={styles.playerStatisticsCatalog__group}
							aria-labelledby={`cat-${group.category}`}
						>
							<header className={styles.playerStatisticsCatalog__groupHeader}>
								<h3 id={`cat-${group.category}`}>
									{group.category.toUpperCase()}
								</h3>
								<span className={styles.playerStatisticsCatalog__groupCount}>
									<span className={styles.srOnly}>
										{`${group.metrics.length} métricas en ${group.category}:`}
									</span>
									<span aria-hidden="true">{group.metrics.length}</span>
								</span>
							</header>

							<ul className={styles.playerStatisticsCatalog__list}>
								{group.metrics.map((item) => {
									const isAbsent = item.value === null;
									const prioritySymbol =
										item.priority === 0 ? "●" : item.priority === 1 ? "○" : "·";
									const priorityClass =
										item.priority === 0
											? styles.dotPrincipal
											: item.priority === 1
												? styles.dotSecundaria
												: styles.dotPocoRelevante;

									return (
										<li
											key={item.key}
											className={`${styles.playerStatisticsCatalog__row} ${
												isAbsent
													? styles.playerStatisticsCatalog__rowAbsent
													: ""
											}`}
										>
											<div
												className={
													styles.playerStatisticsCatalog__metricIdentity
												}
											>
												<span
													className={`${styles.dot} ${priorityClass}`}
													aria-hidden="true"
												>
													{prioritySymbol}
												</span>
												<span className={styles.srOnly}>
													{item.priorityLabel}:
												</span>
												<span
													className={
														styles.playerStatisticsCatalog__metricLabel
													}
													title={item.label}
												>
													{item.label}
												</span>
											</div>

											<div
												className={
													styles.playerStatisticsCatalog__metricValueWrapper
												}
											>
												<span
													className={`${styles.playerStatisticsCatalog__value} ${
														isAbsent
															? styles.playerStatisticsCatalog__valueAbsent
															: ""
													}`}
												>
													{formatStatisticsValue(item.value)}
													{item.unit === "%" && !isAbsent ? " %" : ""}
												</span>
												{item.unit && item.unit !== "%" && !isAbsent && (
													<span
														className={styles.playerStatisticsCatalog__unit}
													>
														{item.unit}
													</span>
												)}
											</div>
										</li>
									);
								})}
							</ul>
						</section>
					))}
				</div>
			)}
		</section>
	);
}
