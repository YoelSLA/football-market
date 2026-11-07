import { Pagination } from "@/shared/components";
import styles from "./RankingsPagination.module.scss";

interface Props {
	currentPage: number; // 1-based
	totalPages: number;
	totalItems: number;
	onPageChange: (page: number) => void;
}

export default function RankingsPagination({
	currentPage,
	totalPages,
	totalItems,
	onPageChange,
}: Props) {
	if (totalItems <= 0 || totalPages <= 0) return null;

	return (
		<div className={styles.paginationContainer}>
			<Pagination
				page={currentPage - 1}
				totalPages={totalPages}
				onChange={(nextZeroBasedPage) => onPageChange(nextZeroBasedPage + 1)}
			/>
		</div>
	);
}
