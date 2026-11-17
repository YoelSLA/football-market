import type {
	CommunityMarketSort,
	CommunityMarketUrlState,
} from "@/features/market/types";

const SORTS = new Set<CommunityMarketSort>(["price", "quantity", "name"]);

export function parseCommunityMarketCriteria(
	params: URLSearchParams,
): CommunityMarketUrlState {
	const sort = params.get("sort") as CommunityMarketSort | null;
	const page = Number(params.get("page"));
	const player = Number(params.get("player"));
	return {
		search: params.get("search")?.trim() ?? "",
		league: params.get("league")?.trim() || "all",
		position: params.get("position")?.trim() || "all",
		sort: sort && SORTS.has(sort) ? sort : "price",
		page: Number.isSafeInteger(page) && page >= 0 ? page : 0,
		player: Number.isSafeInteger(player) && player > 0 ? player : null,
	};
}

export function serializeCommunityMarketCriteria(
	state: CommunityMarketUrlState,
): URLSearchParams {
	const params = new URLSearchParams();
	if (state.search) params.set("search", state.search);
	if (state.league !== "all") params.set("league", state.league);
	if (state.position !== "all") params.set("position", state.position);
	if (state.sort !== "price") params.set("sort", state.sort);
	if (state.page > 0) params.set("page", String(state.page));
	if (state.player !== null) params.set("player", String(state.player));
	return params;
}
