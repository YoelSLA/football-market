import { Navigate, Route, Routes } from "react-router-dom";
import { AppLayout, AuthGuard } from "@/app/layouts";
import { HomePage, LoginPage, RegisterPage } from "@/features/auth";
import {
	MarketBuyPage,
	MarketP2PPage,
	MarketSellPage,
} from "@/features/market";
import {
	PlayerDetailPage,
	PlayerRankingsPage,
	PlayersPage,
} from "@/features/players";
import {
	PortfolioHoldingsPage,
	PortfolioSummaryPage,
	TransactionHistoryPage,
} from "@/features/portfolio";

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
					<Route path="/portfolio" element={<PortfolioSummaryPage />} />
					<Route
						path="/portfolio/holdings"
						element={<PortfolioHoldingsPage />}
					/>
					<Route
						path="/portfolio/transactions"
						element={<TransactionHistoryPage />}
					/>
					<Route path="/players" element={<PlayersPage />} />
					<Route path="/players/ranking" element={<PlayerRankingsPage />} />
					<Route path="/players/:id" element={<PlayerDetailPage />} />
					<Route path="/market/buy" element={<MarketBuyPage />} />
					<Route path="/market/p2p" element={<MarketP2PPage />} />
					<Route path="/market/sell" element={<MarketSellPage />} />
				</Route>
			</Route>

			{/* RUTA NO ENCONTRADA */}
			<Route path="*" element={<Navigate to="/players" replace />} />
		</Routes>
	);
}
