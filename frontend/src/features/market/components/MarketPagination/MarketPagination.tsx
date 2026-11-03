import { Pagination } from "@/shared/components";
import styles from "./MarketPagination.module.scss";

interface Props {
	page: number;
	totalPages: number;
	totalPlayers?: number;
	visiblePlayers: number;
	pageSize?: number;
	onPageChange: (page: number) => void;
}

export default function MarketPagination({
	page,
	totalPages,
	totalPlayers,
	visiblePlayers,
	pageSize,
	onPageChange,
}: Props) {
	return (
		<footer className={styles.marketPagination}>
			<p>
				Mostrando {visiblePlayers} de{" "}
				{totalPlayers?.toLocaleString("es-AR") ?? "—"} jugadores
			</p>
			<Pagination
				page={page}
				totalPages={totalPages}
				onChange={onPageChange}
				className={styles.marketPagination__pages}
			/>
			<label className={styles.marketPagination__size}>
				Jugadores por página
				<select
					disabled
					value={pageSize ?? 12}
					aria-label="Jugadores por página: pendiente"
				>
					<option value={pageSize ?? 12}>{pageSize ?? 12}</option>
				</select>
				<small>Pendiente</small>
			</label>
		</footer>
	);
}
