import { Pagination } from "@/shared/components";
import styles from "./MarketPagination.module.scss";

interface Props {
	page: number;
	totalPages: number;
	totalPlayers?: number;
	visiblePlayers: number;
	pageSize?: number;
	onPageChange: (page: number) => void;
	showSummary?: boolean;
	variant?: "default" | "official";
}

export default function MarketPagination({
	page,
	totalPages,
	totalPlayers,
	visiblePlayers,
	onPageChange,
	showSummary = true,
	variant = "default",
}: Props) {
	return (
		<footer className={styles.marketPagination} data-variant={variant}>
			{showSummary && (
				<p>
					Mostrando {visiblePlayers} de{" "}
					{totalPlayers?.toLocaleString("es-AR") ?? "—"} jugadores
				</p>
			)}
			<Pagination
				page={page}
				totalPages={totalPages}
				onChange={onPageChange}
				className={styles.marketPagination__pages}
			/>
		</footer>
	);
}
