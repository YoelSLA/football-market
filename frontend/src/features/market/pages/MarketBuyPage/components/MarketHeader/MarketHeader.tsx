import MarketPageHeader from "@/features/market/components/MarketPageHeader";
import { TOKEN_SYMBOL } from "@/shared/constants";
import { ShoppingCart } from "@/shared/icons";

interface Props {
	availableCredits?: number;
}

export default function MarketHeader({ availableCredits }: Props) {
	return (
		<MarketPageHeader
			title="Mercado Oficial"
			description={`Comprá ${TOKEN_SYMBOL} directamente a la emisión oficial con la cotización vigente.`}
			icon={ShoppingCart}
			availableCredits={availableCredits}
		/>
	);
}
