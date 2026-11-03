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

/**
 * Información de variación respecto a la cotización previa.
 */
export interface MarketQuoteVariation {
	direction: "positive" | "negative" | "neutral";
	percentageText: string;
	amountText: string;
	arrow: "↗" | "↘" | "→";
	hasPreviousPrice: boolean;
}

const variationFormatter = new Intl.NumberFormat("es-AR", {
	minimumFractionDigits: 1,
	maximumFractionDigits: 1,
});

/**
 * Calcula la variación porcentual y nominal respecto al precio anterior real del backend.
 * Sin asumir ventanas temporales fijas (e.g. 24h).
 */
export function calculateQuoteVariation(
	currentPriceCentimes: number,
	previousPriceCentimes?: number | null,
): MarketQuoteVariation {
	if (
		previousPriceCentimes === null ||
		previousPriceCentimes === undefined ||
		previousPriceCentimes <= 0
	) {
		return {
			direction: "neutral",
			percentageText: "0,0 %",
			amountText: "(0,00 cr.)",
			arrow: "→",
			hasPreviousPrice: false,
		};
	}

	const diffCentimes = currentPriceCentimes - previousPriceCentimes;
	if (diffCentimes === 0) {
		return {
			direction: "neutral",
			percentageText: "0,0 %",
			amountText: "(0,00 cr.)",
			arrow: "→",
			hasPreviousPrice: true,
		};
	}

	const percentage = (Math.abs(diffCentimes) / previousPriceCentimes) * 100;
	const formattedPercentage = variationFormatter.format(percentage);
	const formattedDiffCredits = formatCredits(Math.abs(diffCentimes), true);

	if (diffCentimes > 0) {
		return {
			direction: "positive",
			percentageText: `+${formattedPercentage} %`,
			amountText: `(+${formattedDiffCredits})`,
			arrow: "↗",
			hasPreviousPrice: true,
		};
	}

	return {
		direction: "negative",
		percentageText: `-${formattedPercentage} %`,
		amountText: `(-${formattedDiffCredits})`,
		arrow: "↘",
		hasPreviousPrice: true,
	};
}

/**
 * Formatea la fecha de vigencia a formato local DD/MM/YYYY asegurando interpretación exacta.
 */
export function formatQuoteEffectiveDate(
	effectiveDate: string | null | undefined,
): string {
	if (!effectiveDate) {
		return "—";
	}
	const parts = effectiveDate.split("-");
	if (parts.length === 3 && parts[0].length === 4) {
		const [year, month, day] = parts;
		return `${day}/${month}/${year}`;
	}
	const date = new Date(effectiveDate);
	if (Number.isNaN(date.getTime())) {
		return "—";
	}
	return date.toLocaleDateString("es-AR", {
		day: "2-digit",
		month: "2-digit",
		year: "numeric",
		timeZone: "UTC",
	});
}
