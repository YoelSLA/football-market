import { useQuery } from "@tanstack/react-query";
import { dashboardQueryKeys } from "@/features/dashboard/constants";
import { dashboardService } from "@/features/dashboard/dashboard.service";

export function useDashboardOverviewQuery() {
	return useQuery({
		queryKey: dashboardQueryKeys.overview(),
		queryFn: ({ signal }) => dashboardService.getDashboardOverview(signal),
		staleTime: 30_000,
		refetchOnWindowFocus: true,
	});
}
