import { Navigate, Route, Routes } from "react-router-dom";
import { AppLayout, AuthGuard } from "@/app/layouts";
import { LoginPage, RegisterPage } from "@/features/auth";
import { DashboardPage } from "@/features/dashboard";
import {
	MarketBuyPage,
	MarketOrdersPage,
	MarketP2PPage,
	MarketSellPage,
	MarketTrendsPage,
} from "@/features/market";
import {
	PlayerDetailPage,
	PlayerQuoteHistoryPage,
	PlayerRankingsPage,
	PlayersPage,
} from "@/features/players";
import {
	PortfolioHoldingsPage,
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
					<Route path="/home" element={<DashboardPage />} />
					<Route path="/portfolio" element={<PortfolioHoldingsPage />} />
					<Route
						path="/portfolio/holdings"
						element={<Navigate to="/portfolio" replace />}
					/>
					<Route
						path="/portfolio/transactions"
						element={<TransactionHistoryPage />}
					/>
					<Route path="/players" element={<PlayersPage />} />
					<Route path="/players/ranking" element={<PlayerRankingsPage />} />
					<Route path="/players/:id" element={<PlayerDetailPage />} />
					<Route
						path="/players/:id/history"
						element={<PlayerQuoteHistoryPage />}
					/>
					<Route path="/market/buy" element={<MarketBuyPage />} />
					<Route path="/market/p2p" element={<MarketP2PPage />} />
					<Route path="/market/trends" element={<MarketTrendsPage />} />
					<Route path="/market/sell" element={<MarketSellPage />} />
					<Route path="/market/orders" element={<MarketOrdersPage />} />
				</Route>
			</Route>

			{/* RUTA NO ENCONTRADA */}
			<Route path="*" element={<Navigate to="/players" replace />} />
		</Routes>
	);
}
