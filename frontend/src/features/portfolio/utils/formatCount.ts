/** Devuelve una cantidad con concordancia singular/plural estricta. */
export function formatCount(
	value: number,
	singular: string,
	plural: string,
): string {
	return `${value} ${value === 1 ? singular : plural}`;
}
