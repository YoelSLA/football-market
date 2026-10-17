import { Navigate, Route, Routes } from "react-router-dom";
import { HomePage, LoginPage, RegisterPage } from "@/features/auth";
import { PlayerDetailPage, PlayersPage } from "@/features/players";
import { AppLayout, AuthGuard } from "../layouts";

export default function AppRouter() {
	return (
		<Routes>
			{/* RUTA INICIAL */}
			<Route path="/" element={<Navigate to="/login" replace />} />

			{/* RUTAS PÚBLICAS */}
			<Route element={<AuthGuard privateRoute={false} />}>
				<Route path="/login" element={<LoginPage />} />
				<Route path="/register" element={<RegisterPage />} />
			</Route>

			{/* RUTAS PROTEGIDAS */}
			<Route element={<AuthGuard privateRoute />}>
				<Route element={<AppLayout />}>
					<Route path="/home" element={<HomePage />} />
					<Route path="/players" element={<PlayersPage />} />
					<Route path="/players/:id" element={<PlayerDetailPage />} />
				</Route>
			</Route>

			{/* RUTA NO ENCONTRADA */}
			<Route path="*" element={<Navigate to="/players" replace />} />
		</Routes>
	);
}
