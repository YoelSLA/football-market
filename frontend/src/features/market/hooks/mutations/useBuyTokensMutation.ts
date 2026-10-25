import { useMutation } from "@tanstack/react-query";
import { marketService } from "@/features/market/market.service";
import type { BuyResult } from "@/features/market/types";

/**
 * Mutation hook para comprar tokens directamente al superusuario (SPEC-019).
 * Realiza POST /api/orders/buy con { playerId, quantity }.
 */
export function useBuyTokensMutation() {
	return useMutation<
		BuyResult,
		unknown,
		{ playerId: number; quantity: number }
	>({
		mutationFn: (request) => marketService.buyTokens(request),
	});
}
