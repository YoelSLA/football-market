import MarketPageHeader from "@/features/market/components/MarketPageHeader";
import { ShoppingCart } from "@/shared/icons";

interface Props {
	availableCredits?: number;
}

export default function MarketHeader({ availableCredits }: Props) {
	return (
		<MarketPageHeader
			title="Mercado Oficial"
			description="Comprá tokens directamente a la emisión oficial con la cotización vigente."
			icon={ShoppingCart}
			availableCredits={availableCredits}
		/>
	);
}
