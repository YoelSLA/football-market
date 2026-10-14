import { DEMO_QUOTE_DATES } from "../constants";
import type {
	TokenQuotePoint,
	TokenQuoteSeries,
	TokenQuoteSnapshot,
} from "../types";
import { demoHash } from "../utils";

const MIN_PRICE = 40;
const MAX_PRICE = 180;

function priceFor(playerId: number, index: number): number {
	const spread = MAX_PRICE - MIN_PRICE;

	return MIN_PRICE + (demoHash(playerId, index + 100) % (spread + 1));
}

function buildSnapshot(
	playerId: number,
	points: TokenQuotePoint[],
): TokenQuoteSnapshot {
	const last = points.at(-1);
	const previous = points.at(-2);

	if (!last) {
		return {
			playerId,
			price: null,
			date: null,
			absoluteChange: null,
			percentageChange: null,
			direction: "unknown",
			isDemo: true,
		};
	}

	if (!previous) {
		return {
			playerId,
			price: last.price,
			date: last.date,
			absoluteChange: null,
			percentageChange: null,
			direction: "unknown",
			isDemo: true,
		};
	}

	const absoluteChange = last.price - previous.price;
	const percentageChange =
		previous.price === 0 ? null : (absoluteChange / previous.price) * 100;

	return {
		playerId,
		price: last.price,
		date: last.date,
		absoluteChange,
		percentageChange,
		direction: absoluteChange > 0 ? "up" : absoluteChange < 0 ? "down" : "flat",
		isDemo: true,
	};
}

/**
 * Serie de cotización de demostración: una cotización diaria durante treinta días
 * consecutivos de un período fijo, independiente del reloj del navegador.
 *
 * El precio y la fecha actuales coinciden con el último punto y la variación se deriva
 * del último y el penúltimo; nunca se calculan de forma independiente.
 */
export function getTokenQuoteSeries(
	playerId: number,
	dates: readonly string[] = DEMO_QUOTE_DATES,
): TokenQuoteSeries {
	const points: TokenQuotePoint[] = dates.map((date, index) => ({
		playerId,
		date,
		price: priceFor(playerId, index),
		isDemo: true,
	}));

	return { playerId, points, snapshot: buildSnapshot(playerId, points) };
}
