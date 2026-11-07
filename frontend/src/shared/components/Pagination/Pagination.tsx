import styles from "./Pagination.module.scss";
import PaginationNav from "./PaginationNav";
import PaginationPages from "./PaginationPages";

export interface Props {
	page: number;
	totalPages: number;
	onChange: (page: number) => void;
	/** @deprecated Todas las paginaciones utilizan el mismo diseño. */
	variant?: "bar" | "capsule" | "ranking";
	className?: string;
}

export type PaginationProps = Props;

export default function Pagination({
	page,
	totalPages,
	onChange,
	className,
}: Props) {
	if (totalPages <= 0) return null;

	const currentPage = Math.max(0, Math.min(page, totalPages - 1));

	const goTo = (nextPage: number) => {
		if (nextPage < 0 || nextPage >= totalPages || nextPage === currentPage) {
			return;
		}

		onChange(nextPage);
	};

	const classNames = [styles.pagination, className].filter(Boolean).join(" ");

	return (
		<nav className={classNames} aria-label="Paginación">
			<div className={styles.pagination__content}>
				<PaginationNav
					direction="previous"
					disabled={currentPage === 0}
					onClick={() => goTo(currentPage - 1)}
				/>

				<PaginationPages
					page={currentPage}
					totalPages={totalPages}
					onPageChange={goTo}
				/>

				<PaginationNav
					direction="next"
					disabled={currentPage === totalPages - 1}
					onClick={() => goTo(currentPage + 1)}
				/>
			</div>
		</nav>
	);
}
