import type {
	PlayerQuote,
	PlayerQuoteHistory,
	PlayerQuoteHistoryResponseDTO,
	PlayerQuoteOrigin,
	PlayerQuoteResponseDTO,
} from "../types";

/**
 * Transforma un DTO de cotización individual a modelo de dominio enriquecido
 * con cálculos puros de variación absoluta, variación porcentual y dirección.
 */
export function toPlayerQuote(dto: PlayerQuoteResponseDTO): PlayerQuote {
	const price = Number(dto.price) || 0;
	const previousPrice =
		typeof dto.previousPrice === "number" && Number.isFinite(dto.previousPrice)
			? dto.previousPrice
			: null;

	let absoluteChange: number | null = null;
	let percentageChange: number | null = null;
	let direction: "up" | "down" | "flat" | "unknown" = "unknown";

	if (previousPrice !== null) {
		absoluteChange = price - previousPrice;
		percentageChange =
			previousPrice > 0 ? (absoluteChange / previousPrice) * 100 : 0;
		direction =
			absoluteChange > 0 ? "up" : absoluteChange < 0 ? "down" : "flat";
	}

	const origin: PlayerQuoteOrigin =
		dto.origin === "INITIAL" ||
		dto.origin === "RECOMPUTED" ||
		dto.origin === "CARRIED_FORWARD"
			? dto.origin
			: "RECOMPUTED";

	return {
		effectiveDate: dto.effectiveDate,
		price,
		previousPrice,
		multiplier:
			typeof dto.multiplier === "number"
				? dto.multiplier
				: Number(dto.multiplier) || 1,
		score:
			dto.score !== null && dto.score !== undefined
				? typeof dto.score === "number"
					? dto.score
					: Number(dto.score)
				: null,
		strategy: dto.strategy ?? null,
		strategyVersion: dto.strategyVersion ?? null,
		sourceValuationDate: dto.sourceValuationDate ?? null,
		origin,
		calculatedAt: dto.calculatedAt,
		absoluteChange,
		percentageChange,
		direction,
	};
}

/**
 * Transforma el historial de cotizaciones de un jugador, garantizando orden
 * y derivando la cotización vigente (currentQuote).
 */
export function toPlayerQuoteHistory(
	dto: PlayerQuoteHistoryResponseDTO,
): PlayerQuoteHistory {
	const quotes = Array.isArray(dto?.quotes)
		? dto.quotes.map(toPlayerQuote)
		: [];
	return {
		playerId: dto.playerId,
		quotes,
		currentQuote: quotes.length > 0 ? quotes[0] : null,
	};
}

/**
 * Formatea la variación porcentual con signo y dos decimales (locale es-AR).
 */
export function formatQuotePercentageChange(
	percentageChange: number | null | undefined,
): string {
	if (
		percentageChange === null ||
		percentageChange === undefined ||
		!Number.isFinite(percentageChange)
	) {
		return "—";
	}
	const sign = percentageChange > 0 ? "+" : "";
	const formatted = percentageChange.toLocaleString("es-AR", {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	});
	return `${sign}${formatted} %`;
}

/**
 * Formatea la variación absoluta en créditos con signo y dos decimales (locale es-AR).
 */
export function formatQuoteAbsoluteChange(
	changeInCentimes: number | null | undefined,
): string | null {
	if (
		changeInCentimes === null ||
		changeInCentimes === undefined ||
		!Number.isFinite(changeInCentimes)
	) {
		return null;
	}

	const change = changeInCentimes / 100;
	const sign = change > 0 ? "+" : "";
	const formatted = change.toLocaleString("es-AR", {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	});

	return `${sign}${formatted} ${Math.abs(change) === 1 ? "crédito" : "créditos"}`;
}

/**
 * Formatea un precio en centésimas a créditos institucionales FM con 2 decimales (locale es-AR).
 */
export function formatQuotePrice(
	priceInCentimes: number | null | undefined,
): string {
	if (
		priceInCentimes === null ||
		priceInCentimes === undefined ||
		!Number.isFinite(priceInCentimes)
	) {
		return "—";
	}

	const price = priceInCentimes / 100;

	const formatted = price.toLocaleString("es-AR", {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	});

	return `${formatted} ${price === 1 ? "crédito" : "créditos"}`;
}
