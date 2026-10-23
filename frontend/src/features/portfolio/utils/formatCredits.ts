const credits = new Intl.NumberFormat("es-AR", {
	minimumFractionDigits: 2,
	maximumFractionDigits: 2,
});

/**
 * Conversión de unidad para presentación, no cálculo financiero.
 *
 * La forma compacta se usa donde el espacio es reducido, como en las
 * tarjetas y en el panel de detalle.
 */
export function formatCredits(value: number | null, compact = false): string {
	if (value === null) return "—";
	const amount = credits.format(value / 100);
	return compact ? `${amount} cr.` : `${amount} créditos`;
}
