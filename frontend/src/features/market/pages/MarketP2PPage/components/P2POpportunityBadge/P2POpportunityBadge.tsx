import clsx from "clsx";
import { calculateP2POpportunity } from "@/features/market/market.mapper";
import styles from "./P2POpportunityBadge.module.scss";

interface Props {
	unitPrice: number;
	officialPrice?: number | null;
	className?: string;
}

export default function P2POpportunityBadge({
	unitPrice,
	officialPrice,
	className,
}: Props) {
	const opportunity = calculateP2POpportunity(unitPrice, officialPrice);

	if (opportunity.type === "NEUTRAL") {
		return null;
	}

	return (
		<span
			className={clsx(
				styles.badge,
				opportunity.type === "DISCOUNT"
					? styles.badge_discount
					: styles.badge_premium,
				className,
			)}
		>
			{opportunity.label}
		</span>
	);
}
