const dateTime = new Intl.DateTimeFormat("es-AR", {
	day: "2-digit",
	month: "2-digit",
	year: "numeric",
	hour: "2-digit",
	minute: "2-digit",
});

export function formatDateTime(value: string | null): string {
	return value === null ? "—" : dateTime.format(new Date(value));
}
