export type PageItem =
	| { type: "page"; index: number }
	| { type: "ellipsis"; id: string };

/**
 * Construye la secuencia visible de páginas.
 *
 * - Utiliza índices basados en 0.
 * - Hasta 7 páginas, muestra todas.
 * - Para más de 7 páginas, mantiene 7 elementos visibles.
 * - Conserva siempre la primera y la última página.
 * - Muestra una ventana alrededor de la página actual.
 * - Evita duplicaciones y páginas fuera de rango.
 */
export function buildPageItems(
	current: number,
	totalPages: number,
): PageItem[] {
	if (totalPages <= 0) return [];

	const last = totalPages - 1;
	const safeCurrent = Math.max(0, Math.min(current, last));

	if (totalPages <= 7) {
		return Array.from({ length: totalPages }, (_, index) => ({
			type: "page" as const,
			index,
		}));
	}

	const items: PageItem[] = [];

	const addPage = (index: number) => {
		items.push({ type: "page", index });
	};

	const addEllipsis = (id: string) => {
		items.push({ type: "ellipsis", id });
	};

	/* ================= PRIMERAS PÁGINAS ================= */

	if (safeCurrent <= 3) {
		for (let index = 0; index < 5; index += 1) {
			addPage(index);
		}

		addEllipsis("after");
		addPage(last);

		return items;
	}

	/* ================= ÚLTIMAS PÁGINAS ================= */

	if (safeCurrent >= last - 3) {
		addPage(0);
		addEllipsis("before");

		for (let index = last - 4; index <= last; index += 1) {
			addPage(index);
		}

		return items;
	}

	/* ================= PÁGINAS INTERMEDIAS ================= */

	addPage(0);
	addEllipsis("before");

	for (let index = safeCurrent - 1; index <= safeCurrent + 1; index += 1) {
		addPage(index);
	}

	addEllipsis("after");
	addPage(last);

	return items;
}
