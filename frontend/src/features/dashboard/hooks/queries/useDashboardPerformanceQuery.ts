import { useQuery } from "@tanstack/react-query";
import { dashboardQueryKeys } from "@/features/dashboard/constants";
import { dashboardService } from "@/features/dashboard/dashboard.service";

export function useDashboardPerformanceQuery(range: string = "1M") {
	return useQuery({
		queryKey: dashboardQueryKeys.performance(range),
		queryFn: ({ signal }) =>
			dashboardService.getDashboardPerformance(range, signal),
		staleTime: 30_000,
		refetchOnWindowFocus: true,
	});
}
