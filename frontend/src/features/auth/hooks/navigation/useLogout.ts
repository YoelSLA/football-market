import { useNavigate } from "react-router-dom";
import { useAuth } from "../context";

export function useLogout() {
	const { endSession } = useAuth();
	const navigate = useNavigate();
	return () => {
		endSession();
		navigate("/login", { replace: true });
	};
}
