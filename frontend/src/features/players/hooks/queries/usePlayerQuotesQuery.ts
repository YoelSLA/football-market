import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { playersService } from "@/features/players/players.service";

/**
 * Consulta el historial persistido real de cotizaciones de un jugador.
 * Incluye la lista completa y deriva la cotización vigente.
 */
export function usePlayerQuotesQuery(
	playerId: number | null | undefined,
	enabled = true,
) {
	const validId =
		typeof playerId === "number" && Number.isInteger(playerId) && playerId > 0;

	return useQuery({
		queryKey: playersQueryKeys.quotes(validId ? playerId : 0),
		queryFn: ({ signal }) =>
			playersService.getQuoteHistory(playerId as number, signal),
		enabled: enabled && validId,
		retry: false,
	});
}
