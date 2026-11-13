export type PlayerDetailState =
	| "loading"
	| "invalid-id"
	| "not-found"
	| "error"
	| "ready";

export type StatisticsTab = "quotes" | "summary" | "catalog";

export type QueryState = "loading" | "error" | "ready";

export interface PlayerHeadlineMetric {
	key: string;
	label: string;
	unit: string;
	value: number | null;
}
