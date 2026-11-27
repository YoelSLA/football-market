import MarketPageHeader, {
	type MarketHeaderMetricItem,
} from "@/features/market/components/MarketPageHeader";
import { formatCredits, formatCreditsAmount } from "@/features/market/utils";
import type { PortfolioSummary } from "@/features/portfolio/types";
import { TokenSymbol } from "@/shared/components";
import { TOKEN_SYMBOL } from "@/shared/constants";
import {
	Activity,
	Package,
	ShoppingCart,
	UsersRound,
	Wallet,
} from "@/shared/icons";

interface Props {
	availableCredits?: number;
	totalPlayers?: number;
	priceRange?: { min: number | null; max: number | null };
	portfolioSummary?: PortfolioSummary | null;
}

export default function MarketHeader({
	availableCredits,
	totalPlayers,
	priceRange,
	portfolioSummary,
}: Props) {
	const priceRangeText =
		priceRange?.min != null && priceRange?.max != null
			? `${formatCredits(priceRange.min)} - ${formatCredits(priceRange.max)}`
			: "—";

	const totalPortfolioTokens = portfolioSummary?.totalTokens ?? 0;
	const totalPortfolioPositions = portfolioSummary?.totalPositions ?? 0;

	const metrics: MarketHeaderMetricItem[] = [
		{
			label: "Saldo disponible",
			value:
				availableCredits != null ? formatCreditsAmount(availableCredits) : "—",
			detail: "Poder de compra disponible",
			tooltip: `Saldo disponible en cuenta: ${availableCredits ?? 0} ₵`,
			icon: Wallet,
			variant: "blue",
		},
		{
			label: "Jugadores cotizados",
			value:
				totalPlayers != null
					? `${totalPlayers.toLocaleString("es-AR")} jugadores`
					: "—",
			detail: "Con cotización oficial activa",
			tooltip: `Total de jugadores en el catálogo oficial: ${totalPlayers ?? 0}`,
			icon: UsersRound,
			variant: "indigo",
		},
		{
			label: "Rango de cotización",
			value: priceRangeText,
			detail: "Piso y techo de emisión",
			tooltip: `Rango de cotizaciones oficiales: ${priceRangeText}`,
			icon: Activity,
			variant: "violet",
		},
		{
			label: "Tenencias en cartera",
			value: (
				<>
					{totalPortfolioTokens.toLocaleString("es-AR")}{" "}
					<TokenSymbol size="sm" />
				</>
			),
			detail: `${totalPortfolioPositions} ${
				totalPortfolioPositions === 1 ? "futbolista" : "futbolistas"
			} en cartera`,
			tooltip: `Tokens acumulados en cartera: ${totalPortfolioTokens} en ${totalPortfolioPositions} futbolistas`,
			icon: Package,
			variant: "rose",
		},
	];

	return (
		<MarketPageHeader
			title="Mercado Oficial"
			description={`Comprá ${TOKEN_SYMBOL} directamente a la emisión oficial con la cotización vigente.`}
			icon={ShoppingCart}
			metrics={metrics}
		/>
	);
}
