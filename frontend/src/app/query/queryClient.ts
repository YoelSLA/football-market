import { QueryClient } from "@tanstack/react-query";

const THIRTY_SECONDS = 30 * 1000;

export const queryClient = new QueryClient({
	defaultOptions: {
		queries: {
			retry: 1,
			refetchOnWindowFocus: false,
			staleTime: THIRTY_SECONDS,
		},
	},
});