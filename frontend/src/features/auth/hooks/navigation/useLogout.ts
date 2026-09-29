import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/infrastructure/storage";

export function useLogout() {
	const endSession = useAuthStore((state) => state.endSession);
	const navigate = useNavigate();
	return () => {
		endSession();
		navigate("/login", { replace: true });
	};
}
