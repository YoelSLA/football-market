import { useQuery } from "@tanstack/react-query";
import { authService } from "@/features/auth/auth.service";
import { authQueryKeys } from "@/features/auth/constants";

export function useCurrentUserQuery(enabled: boolean, revision: number) {
	return useQuery({
		queryKey: authQueryKeys.currentUser(revision),
		queryFn: ({ signal }) => authService.currentUser(signal),
		enabled,
		networkMode: "always",
		retry: false,
		staleTime: 5 * 60 * 1000, // 5 minutes - don't refetch unnecessarily
		gcTime: 10 * 60 * 1000,
		refetchOnMount: false,
		refetchOnWindowFocus: false,
		refetchOnReconnect: false,
	});
}
