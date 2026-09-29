import { Navigate, Route, Routes } from "react-router-dom";
import AuthGuard from "@/app/router/AuthGuard";
import { HomePage, LoginPage, RegisterPage } from "@/features/auth";
import { PlayersPage } from "@/features/players";

export default function AppRouter() {
	return (
		<Routes>
			<Route path="/" element={<Navigate to="/login" />} />

			<Route element={<AuthGuard privateRoute={false} />}>
				<Route path="/login" element={<LoginPage />} />
				<Route path="/register" element={<RegisterPage />} />
			</Route>
			<Route element={<AuthGuard privateRoute />}>
				<Route path="/home" element={<HomePage />} />
				<Route path="/players" element={<PlayersPage />} />
			</Route>

			<Route path="*" element={<Navigate to="/players" replace />} />
		</Routes>
	);
}
