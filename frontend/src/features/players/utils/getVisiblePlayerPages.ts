export function getVisiblePlayerPages(
	currentPage: number,
	totalPages: number,
): number[] {
	if (totalPages <= 0) return [];

	const pages = new Set([0, totalPages - 1]);
	const start = Math.max(0, currentPage - 3);
	const end = Math.min(totalPages - 1, currentPage + 3);

	for (let page = start; page <= end; page += 1) pages.add(page);

	return [...pages].sort((first, second) => first - second);
}
