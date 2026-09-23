import { useQuery } from "@tanstack/react-query";
import { authService } from "../services/AuthService";
import { authQueryKeys } from "./authQueryKeys";

export function useCurrentUser(enabled: boolean, revision: number, navigation: string) {
  return useQuery({
    queryKey: authQueryKeys.currentUser(revision, navigation),
    queryFn: ({ signal }) => authService.currentUser(signal),
    enabled,
    networkMode: "always",
    retry: false,
    staleTime: 0,
    gcTime: 0,
    refetchOnMount: "always",
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
}
