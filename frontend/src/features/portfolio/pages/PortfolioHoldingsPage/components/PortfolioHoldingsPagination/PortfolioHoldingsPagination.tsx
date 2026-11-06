import { Pagination } from "@/shared/components";
import styles from "./PortfolioHoldingsPagination.module.scss";

interface Props {
	page: number;
	totalPages: number;
	totalElements: number;
	pageSize: number;
	onPageChange: (page: number) => void;
}

export default function PortfolioHoldingsPagination({
	page,
	totalPages,
	totalElements,
	pageSize,
	onPageChange,
}: Props) {
	if (totalPages <= 0) return null;

	const startElement = page * pageSize + 1;
	const endElement = Math.min((page + 1) * pageSize, totalElements);

	return (
		<div className={styles.pagination}>
			<div className={styles.pagination__info}>
				<span>
					{startElement}–{endElement} de {totalElements} posiciones
				</span>
			</div>

			<div className={styles.pagination__center}>
				<Pagination
					page={page}
					totalPages={totalPages}
					onChange={onPageChange}
				/>
			</div>
		</div>
	);
}
