/**
 * Claves de caché de TanStack Query para Market (SPEC-019).
 * Utiliza claves independientes por jugador para cotización real.
 */
export const marketQueryKeys = {
	all: ["market"] as const,
	currentQuote: (playerId: number) =>
		["market", "current-quote", playerId] as const,
};
