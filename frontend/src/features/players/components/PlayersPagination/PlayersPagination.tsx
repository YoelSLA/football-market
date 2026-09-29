import { getVisiblePlayerPages } from "../../utils";
import styles from "./PlayersPagination.module.scss";

interface PlayersPaginationProps {
	currentPage: number;
	totalPages: number;
	onPageChange: (page: number) => void;
}

export function PlayersPagination({
	currentPage,
	totalPages,
	onPageChange,
}: PlayersPaginationProps) {
	if (totalPages <= 1) return null;

	const lastPage = totalPages - 1;
	const visiblePages = getVisiblePlayerPages(currentPage, totalPages);

	return (
		<nav
			className={styles["players-pagination"]}
			aria-label="Páginas de jugadores"
		>
			<button
				type="button"
				onClick={() => onPageChange(0)}
				disabled={currentPage === 0}
			>
				Primera
			</button>
			{visiblePages.map((page) => (
				<button
					type="button"
					key={page}
					className={
						page === currentPage
							? styles["players-pagination__page--current"]
							: undefined
					}
					aria-current={page === currentPage ? "page" : undefined}
					onClick={() => onPageChange(page)}
				>
					{page + 1}
				</button>
			))}
			<button
				type="button"
				onClick={() => onPageChange(lastPage)}
				disabled={currentPage === lastPage}
			>
				Última
			</button>
		</nav>
	);
}
