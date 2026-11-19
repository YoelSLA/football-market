import type {
	CommunityMarketSort,
	CommunityMarketUrlState,
} from "@/features/market/types";
import { isValidPosition } from "@/features/players";
import {
	centimesToCreditsInput,
	creditsInputToCentimes,
} from "./officialMarketCriteria";

const SORTS = new Set<CommunityMarketSort>(["price", "quantity", "name"]);

export function parseCommunityMarketCriteria(
	params: URLSearchParams,
): CommunityMarketUrlState {
	const sort = params.get("sort") as CommunityMarketSort | null;
	const page = Number(params.get("page"));
	const player = Number(params.get("player"));
	const leagueId = Number(params.get("leagueId"));
	const teamId = Number(params.get("teamId"));
	const parsedMaxPrice = creditsInputToCentimes(params.get("maxPrice") ?? "");
	const rawPosition = params.get("position")?.trim();
	return {
		search: params.get("search")?.trim() ?? "",
		leagueId: Number.isSafeInteger(leagueId) && leagueId > 0 ? leagueId : null,
		teamId: Number.isSafeInteger(teamId) && teamId > 0 ? teamId : null,
		position: rawPosition && isValidPosition(rawPosition) ? rawPosition : null,
		maxPrice:
			parsedMaxPrice !== null && !Number.isNaN(parsedMaxPrice)
				? parsedMaxPrice
				: null,
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
	if (state.leagueId !== null) params.set("leagueId", String(state.leagueId));
	if (state.teamId !== null) params.set("teamId", String(state.teamId));
	if (state.position !== null) params.set("position", state.position);
	if (state.maxPrice !== null)
		params.set("maxPrice", centimesToCreditsInput(state.maxPrice));
	if (state.sort !== "price") params.set("sort", state.sort);
	if (state.page > 0) params.set("page", String(state.page));
	if (state.player !== null) params.set("player", String(state.player));
	return params;
}
