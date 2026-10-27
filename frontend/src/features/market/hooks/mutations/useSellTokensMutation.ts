import { useMutation } from "@tanstack/react-query";
import { marketService } from "@/features/market/market.service";
import type { SellResult } from "@/features/market/types";

export function useSellTokensMutation() {
	return useMutation<
		SellResult,
		unknown,
		{ playerId: number; quantity: number }
	>({
		mutationFn: (request) => marketService.sellTokens(request),
	});
}
