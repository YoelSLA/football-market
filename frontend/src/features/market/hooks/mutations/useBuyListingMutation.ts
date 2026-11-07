import { useMutation } from "@tanstack/react-query";
import { marketService } from "@/features/market/market.service";

export function useBuyListingMutation() {
	return useMutation({
		mutationFn: (listingId: number) => marketService.buyListing(listingId),
		retry: false,
	});
}
