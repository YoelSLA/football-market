import type { PortfolioPosition } from "@/features/portfolio/types";
import {
	PositionCardFooter,
	PositionCardHeader,
	PositionFinancialMetrics,
	PositionTokenMetrics,
} from "./components";
import styles from "./PortfolioPositionCard.module.scss";

interface Props {
	position: PortfolioPosition;
	onDetails: () => void;
}

export default function PortfolioPositionCard({
	position: p,
	onDetails,
}: Props) {
	return (
		<article className={styles.portfolioPosition}>
			<PositionCardHeader
				imageUrl={p.imageUrl}
				playerName={p.playerName}
				teamName={p.teamName}
				position={p.position}
				status={p.status}
			/>

			<PositionTokenMetrics
				quantity={p.quantity}
				availableQuantity={p.availableQuantity}
				reservedQuantity={p.reservedQuantity}
			/>

			<PositionFinancialMetrics
				averageUnitPrice={p.averageUnitPrice}
				currentUnitPrice={p.currentUnitPrice}
				currentValue={p.currentValue}
				valuationStatus={p.valuationStatus}
			/>

			<PositionCardFooter
				result={p.totalProfitLoss}
				playerName={p.playerName}
				playerId={p.playerId}
				onDetails={onDetails}
			/>
		</article>
	);
}
