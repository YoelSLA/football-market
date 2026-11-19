import MarketPageHeader from "@/features/market/components/MarketPageHeader";
import { Puzzle } from "@/shared/icons";

interface Props {
	availableCredits?: number;
}

export default function MarketOrdersHeader({ availableCredits }: Props) {
	return (
		<MarketPageHeader
			title="Mis órdenes"
			description="Gestioná tus publicaciones en el mercado secundario y cancelá ofertas activas."
			icon={Puzzle}
			availableCredits={availableCredits}
		/>
	);
}
