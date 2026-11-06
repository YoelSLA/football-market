import { buildPageItems } from "../utils/buildPageItems";
import PaginationEllipsis from "./PaginationEllipsis";
import PaginationPage from "./PaginationPage";
import styles from "./PaginationPages.module.scss";

interface Props {
	page: number;
	totalPages: number;
	onPageChange: (page: number) => void;
}

export default function PaginationPages({
	page,
	totalPages,
	onPageChange,
}: Props) {
	const items = buildPageItems(page, totalPages);

	return (
		<div className={styles.pages}>
			{items.map((item) =>
				item.type === "ellipsis" ? (
					<PaginationEllipsis key={`ellipsis-${item.id}`} />
				) : (
					<PaginationPage
						key={item.index}
						page={item.index}
						active={item.index === page}
						onClick={() => onPageChange(item.index)}
					/>
				),
			)}
		</div>
	);
}
