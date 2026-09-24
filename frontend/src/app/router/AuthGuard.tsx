import { Navigate, Outlet } from "react-router-dom";
import {
  SessionChecking,
  useAuth,
} from "@/features/auth";

export default function AuthGuard({ privateRoute }: { privateRoute: boolean }) {
  const { status } = useAuth();
  if (status === "unknown" || status === "checking") return <SessionChecking />;
  if (privateRoute && status === "anonymous")
    return <Navigate to="/login" replace />;
  if (!privateRoute && status === "authenticated")
    return <Navigate to="/home" replace />;
  return <Outlet />;
}