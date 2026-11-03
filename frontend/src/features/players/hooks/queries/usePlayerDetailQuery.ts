import { useQuery } from "@tanstack/react-query";
import { playersQueryKeys } from "@/features/players/constants";
import { playersService } from "@/features/players/players.service";

/**
 * Identidad real del detalle. La clave incluye el identificador, de modo que las respuestas
 * de un jugador nunca se reutilizan para otro ni se muestran tras un cambio de id.
 */
export function usePlayerDetailQuery(id: number, enabled = true) {
	return useQuery({
		queryKey: playersQueryKeys.detail(id),
		queryFn: ({ signal }) => playersService.getById(id, signal),
		enabled: enabled && Number.isInteger(id) && id > 0,
		retry: false,
	});
}
