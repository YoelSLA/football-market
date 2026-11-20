/**
 * Claves de caché de TanStack Query para Market (SPEC-019).
 * Utiliza claves independientes por jugador para cotización real.
 */
import type {
	CommunityMarketCriteria,
	OfficialMarketCriteria,
} from "@/features/market/types";

export const marketQueryKeys = {
	all: ["market"] as const,
	officialAll: () => ["market", "official"] as const,
	official: (criteria: OfficialMarketCriteria) =>
		["market", "official", criteria] as const,
	p2pAll: () => ["market", "p2p"] as const,
	p2p: (criteria: CommunityMarketCriteria) =>
		["market", "p2p", criteria] as const,
	currentQuote: (playerId: number) =>
		["market", "current-quote", playerId] as const,
	listings: (playerId?: number) =>
		playerId
			? (["market", "listings", playerId] as const)
			: (["market", "listings"] as const),
	myListingsAll: () => ["market", "listings", "mine"] as const,
	myListings: (params: {
		page: number;
		size?: number;
		status?: string;
		sort?: string;
	}) => ["market", "listings", "mine", params] as const,
};
