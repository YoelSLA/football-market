import { Navigate, Outlet } from "react-router-dom";
import { SessionChecking } from "@/features/auth";
import { AuthStatus, useAuthStore } from "@/infrastructure/storage";

interface Props {
	privateRoute: boolean;
}

export default function AuthGuard({ privateRoute }: Props) {
	const status = useAuthStore((state) => state.status);

	if (status === AuthStatus.UNKNOWN || status === AuthStatus.CHECKING) {
		return <SessionChecking />;
	}

	if (privateRoute && status === AuthStatus.ANONYMOUS) {
		return <Navigate to="/login" replace />;
	}

	if (!privateRoute && status === AuthStatus.AUTHENTICATED) {
		return <Navigate to="/home" replace />;
	}

	return <Outlet />;
}
