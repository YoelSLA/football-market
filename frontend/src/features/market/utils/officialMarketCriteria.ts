import { OFFICIAL_MARKET_DEFAULT_SORT } from "@/features/market/constants";
import type {
	OfficialMarketCriteria,
	OfficialMarketSort,
} from "@/features/market/types";
import { isValidPosition } from "@/features/players";

const SORTS = new Set<OfficialMarketSort>([
	"price,asc",
	"price,desc",
	"variation,asc",
	"variation,desc",
	"name,asc",
	"name,desc",
	"id,asc",
	"id,desc",
]);

const parseNonNegativeInteger = (value: string | null) => {
	if (!value || !/^\d+$/.test(value)) return null;
	const parsed = Number(value);
	return Number.isSafeInteger(parsed) && parsed >= 0 ? parsed : null;
};

const parsePositiveInteger = (value: string | null) => {
	const parsed = parseNonNegativeInteger(value);
	return parsed !== null && parsed > 0 ? parsed : null;
};

export function parseOfficialMarketCriteria(
	params: URLSearchParams,
): OfficialMarketCriteria {
	const sortValue = params.get("sort") as OfficialMarketSort | null;
	const rawPosition = params.get("position")?.trim();

	return {
		name: params.get("name")?.trim() ?? "",
		leagueId: parsePositiveInteger(params.get("leagueId")),
		teamId: parsePositiveInteger(params.get("teamId")),
		position: rawPosition && isValidPosition(rawPosition) ? rawPosition : null,
		nationality: params.get("nationality")?.trim() || null,
		minPrice: parseNonNegativeInteger(params.get("minPrice")),
		maxPrice: parseNonNegativeInteger(params.get("maxPrice")),
		sort:
			sortValue && SORTS.has(sortValue)
				? sortValue
				: OFFICIAL_MARKET_DEFAULT_SORT,
		page: parseNonNegativeInteger(params.get("page")) ?? 0,
	};
}

export function serializeOfficialMarketCriteria(
	criteria: OfficialMarketCriteria,
): URLSearchParams {
	const params = new URLSearchParams();
	if (criteria.name) params.set("name", criteria.name);
	if (criteria.leagueId !== null)
		params.set("leagueId", String(criteria.leagueId));
	if (criteria.teamId !== null) params.set("teamId", String(criteria.teamId));
	if (criteria.position) params.set("position", criteria.position);
	if (criteria.nationality) params.set("nationality", criteria.nationality);
	if (criteria.minPrice !== null)
		params.set("minPrice", String(criteria.minPrice));
	if (criteria.maxPrice !== null)
		params.set("maxPrice", String(criteria.maxPrice));
	if (criteria.sort !== OFFICIAL_MARKET_DEFAULT_SORT)
		params.set("sort", criteria.sort);
	if (criteria.page > 0) params.set("page", String(criteria.page));
	return params;
}

export function creditsInputToCentimes(value: string): number | null {
	const normalized = value.trim().replace(",", ".");
	if (!normalized) return null;
	if (!/^\d+(?:\.\d{1,2})?$/.test(normalized)) return Number.NaN;
	const centimes = Math.round(Number(normalized) * 100);
	return Number.isSafeInteger(centimes) && centimes >= 0
		? centimes
		: Number.NaN;
}

export function centimesToCreditsInput(value: number | null): string {
	return value === null ? "" : (value / 100).toFixed(2).replace(".", ",");
}
