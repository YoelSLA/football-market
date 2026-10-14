export const playersQueryKeys = {
	all: ["players"] as const,
	page: (page: number, size: number) =>
		["players", "page", page, size] as const,
	detail: (id: number) => ["players", "detail", id] as const,
	sampleSeason: (id: number) => ["players", "demo-season", id] as const,
	tokenQuote: (id: number) => ["players", "demo-quotes", id] as const,
};
