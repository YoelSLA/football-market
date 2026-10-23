import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { getTokenQuoteSeries } from "@/features/players/demo";
import type { TokenQuoteSeries } from "@/features/players/types";

/**
 * Cotización y evolución de demostración de un jugador. Solo se consultan cuando la
 * identidad real ya está resuelta; nunca se invocan ante error, 400, 401 o 404.
 */
export function usePlayerTokenQuoteQuery(
	playerId: number | undefined,
	enabled = true,
) {
	return useQuery<TokenQuoteSeries, Error>({
		queryKey: playersQueryKeys.tokenQuote(playerId ?? 0),
		queryFn: () => getTokenQuoteSeries(playerId as number),
		enabled: enabled && typeof playerId === "number",
		staleTime: Number.POSITIVE_INFINITY,
		gcTime: Number.POSITIVE_INFINITY,
		retry: false,
	});
}
