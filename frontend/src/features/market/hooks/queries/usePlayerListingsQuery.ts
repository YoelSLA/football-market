import { useQuery } from "@tanstack/react-query";
import { marketQueryKeys } from "@/features/market/constants";
import { marketService } from "@/features/market/market.service";

export function usePlayerListingsQuery(playerId: number | null) {
	return useQuery({
		queryKey: marketQueryKeys.listings(playerId ?? undefined),
		queryFn: ({ signal }) =>
			marketService.getListings(playerId ?? undefined, signal),
		enabled: playerId !== null,
		retry: false,
	});
}
