import { useMutation } from "@tanstack/react-query";
import { authService } from "../../auth.service";

export function useRegisterMutation() {
	return useMutation({
		mutationFn: authService.register,
		networkMode: "always",
		retry: false,
		gcTime: 0,
	});
}
