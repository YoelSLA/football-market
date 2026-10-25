import { useQueries } from "@tanstack/react-query";
import { marketQueryKeys } from "@/features/market/constants";
import { marketService } from "@/features/market/market.service";
import {
	type MarketQuote,
	MarketQuoteReadError,
	type MarketQuoteStatus,
} from "@/features/market/types";

export interface MarketPlayerQuoteState {
	quote: MarketQuote | null;
	status: MarketQuoteStatus;
	httpStatus?: number;
	isLoading: boolean;
	isError: boolean;
	error: unknown;
	canRetry: boolean;
	onRetry: () => void;
}

/**
 * Consulta las cotizaciones reales vigentes para los jugadores visibles.
 * Utiliza una query por id con la clave propia de market, sin consultas demo.
 */
export function useMarketQuotesQuery(
	playerIds: number[],
	authenticated: boolean,
) {
	const queryResults = useQueries({
		queries: playerIds.map((id) => ({
			queryKey: marketQueryKeys.currentQuote(id),
			queryFn: ({ signal }: { signal: AbortSignal }) =>
				marketService.getCurrentQuote(id, signal),
			staleTime: 30_000,
			enabled: authenticated,
			retry: (failureCount: number, error: unknown) =>
				failureCount < 1 &&
				error instanceof MarketQuoteReadError &&
				(error.reason === "network-error" ||
					(error.reason === "http-error" && (error.httpStatus ?? 0) >= 500)),
		})),
	});

	const quotesByPlayerId = new Map<number, MarketPlayerQuoteState>();

	playerIds.forEach((id, index) => {
		const res = queryResults[index];
		let status: MarketQuoteStatus;
		if (!authenticated) status = "authentication-error";
		else if (res.isPaused) status = "paused";
		else if (res.isPending || res.isFetching) status = "loading";
		else if (res.isError) {
			status =
				res.error instanceof MarketQuoteReadError
					? res.error.reason
					: "network-error";
		} else status = res.data ? "available" : "missing";
		const canRetry = [
			"missing",
			"http-error",
			"network-error",
			"invalid-response",
		].includes(status);
		quotesByPlayerId.set(id, {
			quote: status === "available" ? (res.data ?? null) : null,
			status,
			httpStatus:
				res.error instanceof MarketQuoteReadError
					? res.error.httpStatus
					: undefined,
			isLoading: status === "loading",
			isError: res.isError,
			error: res.error,
			canRetry,
			onRetry: () => {
				if (canRetry) void res.refetch({ cancelRefetch: false });
			},
		});
	});

	const isLoadingAny = queryResults.some((q) => q.isLoading);
	const isErrorAny = queryResults.some((q) => q.isError);

	return {
		quotesByPlayerId,
		isLoadingAny,
		isErrorAny,
		getQuoteState: (id: number): MarketPlayerQuoteState =>
			quotesByPlayerId.get(id) ?? {
				quote: null,
				status: authenticated ? "loading" : "authentication-error",
				isLoading: false,
				isError: false,
				error: null,
				canRetry: false,
				onRetry: () => {},
			},
	};
}
