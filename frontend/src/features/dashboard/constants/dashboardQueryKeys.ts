/**
 * Claves de caché para TanStack Query en la feature dashboard.
 */
export const dashboardQueryKeys = {
	all: ["dashboard"] as const,
	overview: () => [...dashboardQueryKeys.all, "overview"] as const,
	performance: (range?: string) =>
		[...dashboardQueryKeys.all, "performance", range ?? "1M"] as const,
};
