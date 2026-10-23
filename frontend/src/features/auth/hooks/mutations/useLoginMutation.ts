import { useMutation } from "@tanstack/react-query";
import { authService } from "@/features/auth/auth.service";

export function useLoginMutation() {
	return useMutation({
		mutationFn: authService.login,
		networkMode: "always",
		retry: false,
		gcTime: 0,
	});
}
