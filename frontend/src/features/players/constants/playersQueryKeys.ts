export const playersQueryKeys = {
	all: ["players"] as const,
	catalog: (criteria: import("../types").PlayerCatalogCriteria, size: number) =>
		["players", "catalog", criteria, size] as const,
	filters: ["players", "filters"] as const,
	page: (page: number, size: number) =>
		["players", "page", page, size] as const,
	detail: (id: number) => ["players", "detail", id] as const,
	sampleSeason: (id: number) => ["players", "demo-season", id] as const,
	tokenQuote: (id: number) => ["players", "demo-quotes", id] as const,
	statistics: (id: number, seasonKey: string | null) =>
		["players", "statistics", id, seasonKey ?? "auto"] as const,
};
