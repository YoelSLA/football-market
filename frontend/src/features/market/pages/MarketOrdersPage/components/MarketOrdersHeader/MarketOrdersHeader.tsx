import MarketPageHeader, {
	MarketHeaderMetric,
} from "@/features/market/components/MarketPageHeader";
import type { UserOrdersSummary } from "@/features/market/types";
import { formatCreditsAmount } from "@/features/market/utils";
import { TOKEN_SYMBOL } from "@/shared/constants";
import { CheckCircle2, CircleX, Coins, Puzzle, Tag } from "@/shared/icons";

interface Props {
	summary: UserOrdersSummary;
	isLoading?: boolean;
}

export default function MarketOrdersHeader({ summary, isLoading }: Props) {
	const closedCount = summary.cancelledOrders + summary.expiredOrders;

	return (
		<MarketPageHeader
			title="Mis órdenes"
			description="Gestioná tus publicaciones en el mercado secundario y cancelá ofertas activas."
			icon={Puzzle}
		>
			<MarketHeaderMetric
				icon={Tag}
				label="Órdenes activas"
				value={
					isLoading
						? "—"
						: `${summary.activeOrders} ${
								summary.activeOrders === 1 ? "activa" : "activas"
							}`
				}
				title={`${summary.activeTokens} ${TOKEN_SYMBOL} en venta`}
			/>
			<MarketHeaderMetric
				icon={Coins}
				label="Valor en venta"
				value={isLoading ? "—" : formatCreditsAmount(summary.activeValue)}
				title="Importe total en oferta"
			/>
			<MarketHeaderMetric
				icon={CheckCircle2}
				label="Órdenes vendidas"
				value={
					isLoading
						? "—"
						: `${summary.soldOrders} ${
								summary.soldOrders === 1 ? "vendida" : "vendidas"
							}`
				}
				title={`${summary.soldTokens} ${TOKEN_SYMBOL} liquidados`}
			/>
			<MarketHeaderMetric
				icon={CircleX}
				label="Cerradas / Expiradas"
				value={
					isLoading
						? "—"
						: `${closedCount} ${closedCount === 1 ? "cerrada" : "cerradas"}`
				}
				title={`${summary.cancelledOrders} canceladas · ${summary.expiredOrders} expiradas`}
			/>
		</MarketPageHeader>
	);
}
