import { http } from "@/infrastructure/http";
import { toDashboardOverview, toPerformanceSeries } from "./dashboard.mapper";
import type {
	DashboardOverviewResponseDTO,
	DashboardPerformanceResponseDTO,
} from "./types/dtos";
import type { DashboardOverview, PerformanceSeries } from "./types/models";

async function getDashboardOverview(
	signal?: AbortSignal,
): Promise<DashboardOverview> {
	const { data } = await http.get<DashboardOverviewResponseDTO>(
		"/dashboard/overview",
		{
			authenticated: true,
			signal,
		},
	);
	return toDashboardOverview(data);
}

async function getDashboardPerformance(
	range: string = "1M",
	signal?: AbortSignal,
): Promise<PerformanceSeries> {
	const { data } = await http.get<DashboardPerformanceResponseDTO>(
		"/dashboard/performance",
		{
			authenticated: true,
			params: { range },
			signal,
		},
	);
	return toPerformanceSeries(data);
}

export const dashboardService = {
	getDashboardOverview,
	getDashboardPerformance,
};
