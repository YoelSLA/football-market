import { Pagination } from "@/shared/components";
import styles from "./RankingsPagination.module.scss";

interface Props {
	currentPage: number; // 1-based
	totalPages: number;
	totalItems: number;
	itemsOnPage: number;
	pageSize: number;
	pageSizeOptions: number[];
	onPageChange: (page: number) => void;
	onPageSizeChange: (size: number) => void;
}

export default function RankingsPagination({
	currentPage,
	totalPages,
	totalItems,
	itemsOnPage,
	pageSize,
	pageSizeOptions,
	onPageChange,
	onPageSizeChange,
}: Props) {
	if (totalItems <= 0 || totalPages <= 0) return null;

	return (
		<div className={styles.paginationContainer}>
			<p>
				Mostrando {itemsOnPage} de {totalItems} jugadores
			</p>

			<label>
				Filas por página
				<select
					value={pageSize}
					onChange={(event) =>
						onPageSizeChange(Number.parseInt(event.target.value, 10))
					}
				>
					{pageSizeOptions.map((option) => (
						<option key={option} value={option}>
							{option}
						</option>
					))}
				</select>
			</label>

			<Pagination
				page={currentPage - 1}
				totalPages={totalPages}
				onChange={(nextZeroBasedPage) => onPageChange(nextZeroBasedPage + 1)}
			/>
		</div>
	);
}
