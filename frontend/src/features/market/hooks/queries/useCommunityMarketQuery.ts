import { useQuery } from "@tanstack/react-query";
import { marketQueryKeys } from "@/features/market/constants";
import { marketService } from "@/features/market/market.service";

export function useCommunityMarketQuery(enabled = true) {
	return useQuery({
		queryKey: marketQueryKeys.listings(),
		queryFn: ({ signal }) => marketService.getListings(signal),
		enabled,
		retry: false,
	});
}
