import { useQuery } from "@tanstack/react-query";
import { marketQueryKeys } from "@/features/market/constants";
import { marketService } from "@/features/market/market.service";
import type { MyOrdersQueryParams } from "@/features/market/types";

export function useMyListingsQuery(params: MyOrdersQueryParams) {
	return useQuery({
		queryKey: marketQueryKeys.myListings(params),
		queryFn: ({ signal }) => marketService.getMyListings(params, signal),
		retry: false,
	});
}
