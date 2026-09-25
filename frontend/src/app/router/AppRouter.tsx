import { Navigate, Route, Routes } from "react-router-dom";
import AuthGuard from "@/app/router/AuthGuard";
import {
  HomePage,
  LoginPage,
  RegisterPage,
} from "@/features/auth";


export default function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" />} />

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
