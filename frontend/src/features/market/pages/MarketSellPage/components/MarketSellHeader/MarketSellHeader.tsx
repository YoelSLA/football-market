import MarketPageHeader, {
	type MarketHeaderMetricItem,
} from "@/features/market/components/MarketPageHeader";
import { formatCredits } from "@/features/market/utils";
import type { PortfolioSummary } from "@/features/portfolio/types";
import { TokenSymbol } from "@/shared/components";
import { TOKEN_SYMBOL } from "@/shared/constants";
import { CheckCircle2, Coins, Lock, Package, Tag } from "@/shared/icons";
import type { PlayerHolding } from "../../MarketSellPage";

interface Props {
	players: PlayerHolding[];
	portfolioSummary?: PortfolioSummary | null;
}

export default function MarketSellHeader({ players, portfolioSummary }: Props) {
	const totals = players.reduce(
		(accumulator, player) => ({
			total: accumulator.total + player.totalQuantity,
			available: accumulator.available + player.availableQuantity,
			reserved: accumulator.reserved + player.reservedQuantity,
		}),
		{ total: 0, available: 0, reserved: 0 },
	);

	const portfolioValueText =
		portfolioSummary?.totalCurrentValue != null
			? formatCredits(portfolioSummary.totalCurrentValue)
			: "—";

	const metrics: MarketHeaderMetricItem[] = [
		{
			label: "Disponibles para venta",
			value: (
				<>
					{totals.available.toLocaleString("es-AR")} <TokenSymbol size="sm" />
				</>
			),
			detail: "Listos para vender o publicar",
			tooltip: `${totals.available} tokens libres para operar`,
			icon: CheckCircle2,
			variant: "blue",
		},
		{
			label: "Comprometidos en P2P",
			value: (
				<>
					{totals.reserved.toLocaleString("es-AR")} <TokenSymbol size="sm" />
				</>
			),
			detail: "En órdenes de venta activas",
			tooltip: `${totals.reserved} tokens bloqueados en publicaciones activas`,
			icon: Lock,
			variant: "indigo",
		},
		{
			label: "Valor de cartera",
			value: portfolioValueText,
			detail: "Valuación a cotización vigente",
			tooltip: `Valor actual de tus activos: ${portfolioValueText}`,
			icon: Coins,
			variant: "violet",
		},
		{
			label: "Activos en cartera",
			value: `${players.length} ${
				players.length === 1 ? "futbolista" : "futbolistas"
			}`,
			detail: (
				<>
					{totals.total.toLocaleString("es-AR")} <TokenSymbol size="sm" />{" "}
					totales
				</>
			),
			tooltip: `${players.length} posiciones distintas con un total de ${totals.total} tokens`,
			icon: Package,
			variant: "rose",
		},
	];

	return (
		<MarketPageHeader
			title="Publicar venta"
			description={`Publicá tus ${TOKEN_SYMBOL} en el mercado secundario o vendé a la emisión oficial.`}
			icon={Tag}
			metrics={metrics}
		/>
	);
}
