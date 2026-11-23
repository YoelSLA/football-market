import { useMutation } from "@tanstack/react-query";
import { marketService } from "@/features/market/market.service";
import type { SellOfficialResult } from "@/features/market/types";

export function useSellOfficialTokensMutation() {
	return useMutation<
		SellOfficialResult,
		unknown,
		{ playerId: number; quantity: number }
	>({
		mutationFn: (request) => marketService.sellTokensOfficial(request),
	});
}
