import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { playersService } from "@/features/players/players.service";

/**
 * Consulta exclusivamente la cotización vigente del jugador.
 * Retorna null si el jugador nunca fue cotizado (QUOTE_NOT_FOUND).
 */
export function usePlayerCurrentQuoteQuery(
	playerId: number | null | undefined,
	enabled = true,
) {
	const validId =
		typeof playerId === "number" && Number.isInteger(playerId) && playerId > 0;

	return useQuery({
		queryKey: playersQueryKeys.currentQuote(validId ? playerId : 0),
		queryFn: ({ signal }) =>
			playersService.getCurrentQuote(playerId as number, signal),
		enabled: enabled && validId,
		retry: false,
	});
}
