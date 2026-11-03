import { Button } from "@/shared/components";
import { ChevronLeft, ChevronRight } from "@/shared/icons";

import styles from "./PortfolioHoldingsPagination.module.scss";

interface Props {
	page: number;
	totalPages: number;
	totalElements: number;
	pageSize: number;
	onPageChange: (page: number) => void;
}

type PageItem =
	| { type: "page"; index: number }
	| { type: "ellipsis"; id: string };

function buildPageItems(current: number, totalPages: number) {
	if (totalPages <= 7) {
		return Array.from({ length: totalPages }, (_, index) => ({
			type: "page" as const,
			index,
		}));
	}

	const items: PageItem[] = [];

	const windowStart = Math.max(1, current - 1);
	const windowEnd = Math.min(totalPages - 2, current + 1);

	items.push({ type: "page" as const, index: 0 });

	if (windowStart > 1) {
		const omittedBefore = windowStart - 1;
		if (omittedBefore >= 2)
			items.push({ type: "ellipsis" as const, id: "before" });
		else items.push({ type: "page" as const, index: omittedBefore });
	}

	for (let index = windowStart; index <= windowEnd; index += 1) {
		items.push({ type: "page" as const, index });
	}

	if (windowEnd < totalPages - 2) {
		const omittedAfter = totalPages - 1 - windowEnd - 1;
		if (omittedAfter >= 2)
			items.push({ type: "ellipsis" as const, id: "after" });
		else items.push({ type: "page" as const, index: totalPages - 2 });
	}

	items.push({ type: "page" as const, index: totalPages - 1 });

	return items;
}

export default function PortfolioHoldingsPagination({
	page,
	totalPages,
	totalElements,
	pageSize,
	onPageChange,
}: Props) {
	if (totalPages <= 0) return null;

	const goTo = (nextPage: number) => {
		if (nextPage < 0 || nextPage >= totalPages || nextPage === page) {
			return;
		}
		onPageChange(nextPage);
	};

	const items = buildPageItems(page, totalPages);
	const startElement = page * pageSize + 1;
	const endElement = Math.min((page + 1) * pageSize, totalElements);

	return (
		<nav className={styles.pagination} aria-label="Paginación de tenencias">
			<div className={styles.pagination__info}>
				<span>
					{startElement}–{endElement} de {totalElements} posiciones
				</span>
			</div>

			<div className={styles.pagination__center}>
				<Button
					type="button"
					className={styles.pagination__nav}
					onClick={() => goTo(page - 1)}
					disabled={page === 0}
					aria-label="Página anterior"
				>
					<ChevronLeft aria-hidden="true" />
				</Button>

				<div className={styles.pagination__pages}>
					{items.map((item) =>
						item.type === "ellipsis" ? (
							<span
								key={`ellipsis-${item.id}`}
								className={styles.pagination__ellipsis}
								aria-hidden="true"
							>
								…
							</span>
						) : (
							<Button
								key={item.index}
								type="button"
								className={[
									styles.pagination__page,
									item.index === page && styles.pagination__page_active,
								]
									.filter(Boolean)
									.join(" ")}
								onClick={() => goTo(item.index)}
								aria-label={`Página ${item.index + 1}`}
								aria-current={item.index === page ? "page" : undefined}
							>
								{item.index + 1}
							</Button>
						),
					)}
				</div>

				<Button
					type="button"
					className={styles.pagination__nav}
					onClick={() => goTo(page + 1)}
					disabled={page === totalPages - 1}
					aria-label="Página siguiente"
				>
					<ChevronRight aria-hidden="true" />
				</Button>
			</div>
		</nav>
	);
}
