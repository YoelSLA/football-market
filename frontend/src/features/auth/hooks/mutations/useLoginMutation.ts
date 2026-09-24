import { useMutation } from "@tanstack/react-query";
import { authService } from "../../services";

export function useLoginMutation() {
	return useMutation({
		mutationFn: authService.login,
		networkMode: "always",
		retry: false,
		gcTime: 0,
	});
}
