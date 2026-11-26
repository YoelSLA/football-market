import MarketPageHeader, {
	MarketHeaderMetric,
} from "@/features/market/components/MarketPageHeader";
import { TokenSymbol } from "@/shared/components";
import { TOKEN_SYMBOL } from "@/shared/constants";
import { CheckCircle2, Lock, Package, Tag } from "@/shared/icons";
import type { PlayerHolding } from "../../MarketSellPage";

interface Props {
	players: PlayerHolding[];
}

export default function MarketSellHeader({ players }: Props) {
	const totals = players.reduce(
		(accumulator, player) => ({
			total: accumulator.total + player.totalQuantity,
			available: accumulator.available + player.availableQuantity,
			reserved: accumulator.reserved + player.reservedQuantity,
		}),
		{ total: 0, available: 0, reserved: 0 },
	);

	return (
		<MarketPageHeader
			title="Publicar venta"
			description={`Publicá tus ${TOKEN_SYMBOL} en el mercado secundario.`}
			icon={Tag}
		>
			<MarketHeaderMetric
				icon={Package}
				label="Total tenencias"
				value={
					<>
						{totals.total.toLocaleString("es-AR")} <TokenSymbol size="sm" />
					</>
				}
				title="Total de tokens en cartera"
			/>
			<MarketHeaderMetric
				icon={CheckCircle2}
				label="Disponibles"
				value={
					<>
						{totals.available.toLocaleString("es-AR")} <TokenSymbol size="sm" />
					</>
				}
				title="Tokens disponibles para publicar"
			/>
			<MarketHeaderMetric
				icon={Lock}
				label="Reservados"
				value={
					<>
						{totals.reserved.toLocaleString("es-AR")} <TokenSymbol size="sm" />
					</>
				}
				title="Tokens en órdenes de venta activas"
			/>
		</MarketPageHeader>
	);
}
