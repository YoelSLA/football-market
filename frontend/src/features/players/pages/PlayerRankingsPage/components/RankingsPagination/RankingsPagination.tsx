import { ChevronLeft, ChevronRight } from "@/shared/icons";
import styles from "./RankingsPagination.module.scss";

interface Props {
	currentPage: number; // 1-based
	totalPages: number;
	totalItems: number;
	onPageChange: (page: number) => void;
}

type PaginationItem =
	| { type: "page"; page: number; key: string }
	| { type: "ellipsis"; key: string };

export default function RankingsPagination({
	currentPage,
	totalPages,
	totalItems,
	onPageChange,
}: Props) {
	if (totalItems <= 0) return null;

	const getPageNumbers = (): PaginationItem[] => {
		const items: PaginationItem[] = [];
		if (totalPages <= 7) {
			for (let i = 1; i <= totalPages; i += 1) {
				items.push({ type: "page", page: i, key: `page-${i}` });
			}
			return items;
		}

		items.push({ type: "page", page: 1, key: "page-1" });
		if (currentPage > 3) {
			items.push({ type: "ellipsis", key: "ellipsis-start" });
		}

		const start = Math.max(2, currentPage - 1);
		const end = Math.min(totalPages - 1, currentPage + 1);

		for (let i = start; i <= end; i += 1) {
			items.push({ type: "page", page: i, key: `page-${i}` });
		}

		if (currentPage < totalPages - 2) {
			items.push({ type: "ellipsis", key: "ellipsis-end" });
		}
		items.push({
			type: "page",
			page: totalPages,
			key: `page-${totalPages}`,
		});

		return items;
	};

	const pages = getPageNumbers();

	return (
		<nav
			className={styles.paginationContainer}
			aria-label="Controles de paginación del ranking"
		>
			<div className={styles.centerNav}>
				<button
					type="button"
					className={styles.navButton}
					onClick={() => onPageChange(currentPage - 1)}
					disabled={currentPage <= 1}
					aria-label="Página anterior"
				>
					<ChevronLeft size={16} aria-hidden="true" />
				</button>

				<div className={styles.pageButtons}>
					{pages.map((item) =>
						item.type === "ellipsis" ? (
							<span
								key={item.key}
								className={styles.ellipsis}
								aria-hidden="true"
							>
								…
							</span>
						) : (
							<button
								key={item.key}
								type="button"
								className={`${styles.pageButton} ${
									item.page === currentPage ? styles.pageButton_active : ""
								}`}
								onClick={() => onPageChange(item.page)}
								aria-current={item.page === currentPage ? "page" : undefined}
								aria-label={`Página ${item.page}`}
							>
								{item.page}
							</button>
						),
					)}
				</div>

				<button
					type="button"
					className={styles.navButton}
					onClick={() => onPageChange(currentPage + 1)}
					disabled={currentPage >= totalPages}
					aria-label="Página siguiente"
				>
					<ChevronRight size={16} aria-hidden="true" />
				</button>
			</div>
		</nav>
	);
}
