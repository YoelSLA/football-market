import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { marketQueryKeys } from "@/features/market/constants";
import { marketService } from "@/features/market/market.service";
import type { CommunityMarketCriteria } from "@/features/market/types";

export function useP2PMarketQuery(
	criteria: CommunityMarketCriteria,
	enabled = true,
) {
	return useQuery({
		queryKey: marketQueryKeys.p2p(criteria),
		queryFn: ({ signal }) => marketService.getP2PMarket(criteria, signal),
		enabled,
		placeholderData: keepPreviousData,
		retry: false,
	});
}
