import { useQuery } from "@tanstack/react-query";
import { marketQueryKeys } from "@/features/market/constants";
import { marketService } from "@/features/market/market.service";

/**
 * Consulta reactiva de tendencias y sentimiento del mercado (SPEC-029).
 * Incluye auto-refresco cada 60 segundos (solo en pestaña activa)
 * y soporte para refresco manual forzado mediante refetch().
 */
export function useMarketTrendsQuery() {
	return useQuery({
		queryKey: marketQueryKeys.trends(),
		queryFn: ({ signal }) => marketService.getTrends(signal),
		staleTime: 30_000,
		refetchInterval: 60_000,
		refetchIntervalInBackground: false,
	});
}
