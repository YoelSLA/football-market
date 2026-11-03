const percentage = new Intl.NumberFormat("es-AR", {
	minimumFractionDigits: 2,
	maximumFractionDigits: 2,
});

export function formatPercentage(value: number | null): string {
	return value === null ? "—" : `${percentage.format(value)} %`;
}
