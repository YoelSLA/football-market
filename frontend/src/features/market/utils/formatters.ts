import type { BuyEstimation } from "../types";

const creditsFormatter = new Intl.NumberFormat("es-AR", {
	minimumFractionDigits: 2,
	maximumFractionDigits: 2,
});

const centimesFormatter = new Intl.NumberFormat("es-AR", {
	maximumFractionDigits: 0,
});

const dateTimeFormatter = new Intl.DateTimeFormat("es-AR", {
	day: "2-digit",
	month: "2-digit",
	year: "numeric",
	hour: "2-digit",
	minute: "2-digit",
});

/**
 * Formatea un valor en centésimas a créditos con 2 decimales y coma (locale es-AR).
 */
export function formatCredits(
	centimes: number | null | undefined,
	compact = false,
): string {
	if (centimes === null || centimes === undefined || Number.isNaN(centimes)) {
		return "—";
	}
	const formatted = creditsFormatter.format(centimes / 100);
	return compact ? `${formatted} cr.` : `${formatted} créditos`;
}

/**
 * Formatea un valor en centésimas agrupando millares con punto (locale es-AR).
 */
export function formatCentimes(centimes: number | null | undefined): string {
	if (centimes === null || centimes === undefined || Number.isNaN(centimes)) {
		return "—";
	}
	return `${centimesFormatter.format(centimes)} centésimas`;
}

/**
 * Formatea la fecha y hora reales de cálculo de la cotización persistida.
 * No genera fechas simuladas ni sustituye con Date.now().
 */
export function formatQuoteCalculatedAt(
	calculatedAt: string | null | undefined,
): string {
	if (!calculatedAt) {
		return "Cotización no disponible";
	}
	const date = new Date(calculatedAt);
	if (Number.isNaN(date.getTime())) {
		return "Cotización no disponible";
	}
	return `Cotización calculada el ${dateTimeFormatter.format(date)}`;
}

/**
 * Calcula la estimación previa a la compra basada en la cotización vigente.
 */
export function calculateEstimation(
	quantity: number,
	unitPriceCentimes: number,
): BuyEstimation {
	return {
		quantity,
		unitPriceCentimes,
		totalCentimes: quantity * unitPriceCentimes,
	};
}
