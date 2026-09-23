import { Navigate, Outlet, Route, Routes } from "react-router-dom";
import { HomePage, LoginPage, RegisterPage, SessionChecking, useAuth } from "@/features/auth";

function AuthGuard({ privateRoute }: { privateRoute: boolean }) {
  const { status } = useAuth();
  if (status === "unknown" || status === "checking") return <SessionChecking />;
  if (privateRoute && status === "anonymous") return <Navigate to="/login" replace />;
  if (!privateRoute && status === "authenticated") return <Navigate to="/home" replace />;
  return <Outlet />;
}

export default function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/players" replace />} />

      <Route path="/players" element={<div>Players</div>} />

      <Route element={<AuthGuard privateRoute={false} />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route element={<AuthGuard privateRoute />}>
        <Route path="/home" element={<HomePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/players" replace />} />
    </Routes>
  );
}
