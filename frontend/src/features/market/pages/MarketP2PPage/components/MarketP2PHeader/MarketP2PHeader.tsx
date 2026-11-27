import MarketPageHeader, {
	type MarketHeaderMetricItem,
} from "@/features/market/components/MarketPageHeader";
import { formatCredits, formatCreditsAmount } from "@/features/market/utils";
import { Activity, TrendingDown, UsersRound, Wallet } from "@/shared/icons";

interface Props {
	availableCredits?: number;
	totalPlayers?: number;
	priceRange?: { min: number | null; max: number | null };
	totalP2PTokens?: number;
}

export default function MarketP2PHeader({
	availableCredits,
	totalPlayers,
	priceRange,
	totalP2PTokens = 0,
}: Props) {
	const minPriceText =
		priceRange?.min != null ? formatCredits(priceRange.min) : "—";

	const rangeText =
		priceRange?.min != null && priceRange?.max != null
			? `${formatCredits(priceRange.min)} - ${formatCredits(priceRange.max)}`
			: "—";

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
			label: "Oferta disponible",
			value:
				totalPlayers != null
					? `${totalPlayers.toLocaleString("es-AR")} jugadores`
					: "—",
			detail:
				totalP2PTokens > 0
					? `${totalP2PTokens.toLocaleString("es-AR")} tokens en venta`
					: "Lotes publicados por usuarios",
			tooltip: `Jugadores con publicaciones activas en P2P: ${totalPlayers ?? 0}`,
			icon: UsersRound,
			variant: "indigo",
		},
		{
			label: "Mejor precio P2P",
			value: minPriceText,
			detail: "Piso de oferta en el mercado",
			tooltip: `Cotización más accesible en el mercado secundario: ${minPriceText}`,
			icon: TrendingDown,
			variant: "violet",
		},
		{
			label: "Dispersión de precio",
			value: rangeText,
			detail: "Banda de precios comunitarios",
			tooltip: `Rango de precios publicados: ${rangeText}`,
			icon: Activity,
			variant: "rose",
		},
	];

	return (
		<MarketPageHeader
			title="Mercado P2P"
			description="Compará publicaciones y comprá lotes completos de otros usuarios."
			icon={UsersRound}
			metrics={metrics}
		/>
	);
}
