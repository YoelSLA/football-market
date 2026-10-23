import Button from "@/shared/components/Button";
import styles from "./Pagination.module.scss";

interface Props {
	page: number;
	totalPages: number;
	onChange: (page: number) => void;
	/** "bar" mantiene la barra de ancho completo; "capsule" la version flotante. */
	variant?: "bar" | "capsule";
}

type PageItem =
	| { type: "page"; index: number }
	| { type: "ellipsis"; id: string };

/**
 * Construye la secuencia visible de paginas.
 * - Con pocas paginas se muestran todas.
 * - Siempre quedan visibles la primera y la ultima.
 * - La ventana alrededor de la pagina actual es actual-1 .. actual+1.
 * - Elipsis solo cuando se omiten 2 o mas paginas, de modo que nunca
 *   queda un elipsis pegado a una pagina que si se muestra.
 */
function buildPageItems(current: number, totalPages: number): PageItem[] {
	const last = totalPages - 1;

	if (totalPages <= 7) {
		return Array.from({ length: totalPages }, (_, index) => ({
			type: "page" as const,
			index,
		}));
	}

	const items: PageItem[] = [];
	const addPage = (index: number) => items.push({ type: "page", index });
	const addEllipsis = (id: string) => items.push({ type: "ellipsis", id });

	const windowStart = Math.max(1, current - 1);
	const windowEnd = Math.min(last - 1, current + 1);

	addPage(0);

	if (windowStart > 1) {
		const omittedBefore = windowStart - 1;

		if (omittedBefore >= 2) addEllipsis("before");
		else addPage(omittedBefore);
	}

	for (let index = windowStart; index <= windowEnd; index += 1) {
		addPage(index);
	}

	if (windowEnd < last - 1) {
		const omittedAfter = last - windowEnd - 1;

		if (omittedAfter >= 2) addEllipsis("after");
		else addPage(last - 1);
	}

	addPage(last);

	return items;
}

export default function Pagination({
	page,
	totalPages,
	onChange,
	variant = "bar",
}: Props) {
	if (totalPages <= 0) return null;

	const goTo = (nextPage: number) => {
		if (nextPage < 0 || nextPage >= totalPages || nextPage === page) {
			return;
		}

		onChange(nextPage);
	};

	const items = buildPageItems(page, totalPages);

	return (
		<nav
			className={
				variant === "capsule"
					? `${styles.pagination} ${styles.pagination_capsule}`
					: styles.pagination
			}
			aria-label="Paginación"
		>
			<Button
				type="button"
				className={styles.pagination__nav}
				onClick={() => goTo(page - 1)}
				disabled={page === 0}
				aria-label="Página anterior"
			>
				‹
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
				›
			</Button>
		</nav>
	);
}
