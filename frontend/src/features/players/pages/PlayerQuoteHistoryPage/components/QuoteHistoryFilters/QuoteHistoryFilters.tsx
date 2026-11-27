import { Download, X } from "@/shared/icons";
import styles from "./QuoteHistoryFilters.module.scss";

export type DirectionFilter = "ALL" | "UP" | "DOWN" | "FLAT";

interface Props {
	directionFilter: DirectionFilter;
	onDirectionChange: (dir: DirectionFilter) => void;
	startDate: string;
	endDate: string;
	onStartDateChange: (date: string) => void;
	onEndDateChange: (date: string) => void;
	onClearFilters: () => void;
	onExportCSV: () => void;
	counts: {
		all: number;
		up: number;
		down: number;
		flat: number;
	};
	canExport: boolean;
}

export default function QuoteHistoryFilters({
	directionFilter,
	onDirectionChange,
	startDate,
	endDate,
	onStartDateChange,
	onEndDateChange,
	onClearFilters,
	onExportCSV,
	counts,
	canExport,
}: Props) {
	const hasActiveFilters =
		directionFilter !== "ALL" || Boolean(startDate) || Boolean(endDate);

	return (
		<search className={styles.toolbar} aria-label="Filtros del historial">
			<div className={styles.toolbar__left}>
				<fieldset
					className={styles.toolbar__pills}
					aria-label="Filtrar por variación"
				>
					<button
						type="button"
						className={`${styles.toolbar__pill} ${
							directionFilter === "ALL" ? styles.toolbar__pill_active : ""
						}`}
						onClick={() => onDirectionChange("ALL")}
					>
						Todas ({counts.all})
					</button>
					<button
						type="button"
						className={`${styles.toolbar__pill} ${
							directionFilter === "UP" ? styles.toolbar__pill_active : ""
						}`}
						onClick={() => onDirectionChange("UP")}
					>
						Subidas ({counts.up})
					</button>
					<button
						type="button"
						className={`${styles.toolbar__pill} ${
							directionFilter === "DOWN" ? styles.toolbar__pill_active : ""
						}`}
						onClick={() => onDirectionChange("DOWN")}
					>
						Bajadas ({counts.down})
					</button>
					<button
						type="button"
						className={`${styles.toolbar__pill} ${
							directionFilter === "FLAT" ? styles.toolbar__pill_active : ""
						}`}
						onClick={() => onDirectionChange("FLAT")}
					>
						Estables ({counts.flat})
					</button>
				</fieldset>

				<span className={styles.toolbar__divider} aria-hidden="true" />

				<div className={styles.toolbar__dates}>
					<div className={styles.toolbar__dateGroup}>
						<label
							htmlFor="quote-history-start-date"
							className={styles.toolbar__dateLabel}
						>
							Desde
						</label>
						<input
							id="quote-history-start-date"
							type="date"
							value={startDate}
							onChange={(e) => onStartDateChange(e.target.value)}
							className={styles.toolbar__dateInput}
							aria-label="Fecha desde"
						/>
					</div>

					<div className={styles.toolbar__dateGroup}>
						<label
							htmlFor="quote-history-end-date"
							className={styles.toolbar__dateLabel}
						>
							Hasta
						</label>
						<input
							id="quote-history-end-date"
							type="date"
							value={endDate}
							onChange={(e) => onEndDateChange(e.target.value)}
							className={styles.toolbar__dateInput}
							aria-label="Fecha hasta"
						/>
					</div>
				</div>
			</div>

			<div className={styles.toolbar__right}>
				<button
					type="button"
					className={styles.toolbar__exportBtn}
					onClick={onExportCSV}
					disabled={!canExport}
					title="Descargar historial en formato CSV"
				>
					<Download size={13} aria-hidden="true" />
					<span>Exportar CSV</span>
				</button>

				{hasActiveFilters && (
					<button
						type="button"
						className={styles.toolbar__clearBtn}
						onClick={onClearFilters}
						title="Restablecer filtros"
					>
						<X size={13} aria-hidden="true" />
						<span>Limpiar</span>
					</button>
				)}
			</div>
		</search>
	);
}
