/**
 * Formatea una fecha en formato ISO/string (YYYY-MM-DD) a formato legible (DD/MM/YYYY).
 * Si la fecha no es válida o está ausente, retorna el valor de reserva (por defecto "No disponible").
 */
export function formatDate(
	dateStr: string | null | undefined,
	fallback = "No disponible",
): string {
	if (!dateStr) return fallback;

	const parts = dateStr.split("-");
	if (parts.length === 3) {
		const [year, month, day] = parts;
		return `${day}/${month}/${year}`;
	}

	return dateStr;
}
