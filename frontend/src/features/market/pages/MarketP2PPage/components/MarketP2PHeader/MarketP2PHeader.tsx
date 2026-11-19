import MarketPageHeader from "@/features/market/components/MarketPageHeader";
import { UsersRound } from "@/shared/icons";

interface Props {
	availableCredits?: number;
}

export default function MarketP2PHeader({ availableCredits }: Props) {
	return (
		<MarketPageHeader
			title="Mercado P2P"
			description="Compará publicaciones y comprá lotes completos de otros usuarios."
			icon={UsersRound}
			availableCredits={availableCredits}
		/>
	);
}
