import { Navigate, Outlet } from "react-router-dom";
import {
  SessionChecking,
} from "@/features/auth";
import { useAuthStore } from "@/infrastructure/storage/useAuthStore";
import { AuthStatus } from "@/infrastructure/storage/types";

export default function AuthGuard({
  privateRoute,
}: {
  privateRoute: boolean;
}) {
  const status = useAuthStore((state) => state.status);

  if (
    status === AuthStatus.UNKNOWN ||
    status === AuthStatus.CHECKING
  ) {
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