export const playersQueryKeys = {
	all: ["players"] as const,
	page: (page: number, size: number) =>
		["players", "page", page, size] as const,
};
