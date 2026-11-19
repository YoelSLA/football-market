import { useMutation, useQueryClient } from "@tanstack/react-query";
import { marketQueryKeys } from "@/features/market/constants";
import { marketService } from "@/features/market/market.service";

export function useCancelListingMutation() {
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: (listingId: number) => marketService.cancelListing(listingId),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: marketQueryKeys.all });
			queryClient.invalidateQueries({ queryKey: ["portfolio"] });
		},
		retry: false,
	});
}
