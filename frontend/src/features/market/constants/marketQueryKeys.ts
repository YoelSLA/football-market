/**
 * Claves de caché de TanStack Query para Market (SPEC-019).
 * Utiliza claves independientes por jugador para cotización real.
 */
import type { OfficialMarketCriteria } from "@/features/market/types";

export const marketQueryKeys = {
	all: ["market"] as const,
	officialAll: () => ["market", "official"] as const,
	official: (criteria: OfficialMarketCriteria) =>
		["market", "official", criteria] as const,
	currentQuote: (playerId: number) =>
		["market", "current-quote", playerId] as const,
	listings: () => ["market", "listings"] as const,
};
