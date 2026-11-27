import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { marketQueryKeys } from "@/features/market/constants";
import { marketService } from "@/features/market/market.service";
import type { OfficialMarketCriteria } from "@/features/market/types";

export function useOfficialMarketQuery(
	criteria: OfficialMarketCriteria,
	enabled = true,
) {
	return useQuery({
		queryKey: marketQueryKeys.official(criteria),
		queryFn: ({ signal }) => marketService.getOfficialMarket(criteria, signal),
		enabled,
		placeholderData: keepPreviousData,
		retry: false,
	});
}
