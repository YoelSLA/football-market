import { formatPercentage } from "@/features/portfolio/utils";
import { TOKEN_SYMBOL } from "@/shared/constants";
import styles from "./PositionTenureTrack.module.scss";

interface Props {
	quantity: number;
	availableQuantity: number;
	reservedQuantity: number;
}

export default function PositionTenureTrack({
	quantity,
	availableQuantity,
	reservedQuantity,
}: Props) {
	const available = quantity > 0 ? (availableQuantity / quantity) * 100 : 0;
	const reserved = quantity > 0 ? (reservedQuantity / quantity) * 100 : 0;

	return (
		<>
			<div
				className={styles.track}
				role="img"
				aria-label={`${availableQuantity} disponibles y ${reservedQuantity} reservados de ${quantity} ${TOKEN_SYMBOL}`}
			>
				<span style={{ width: `${available}%` }} />
				<span style={{ width: `${reserved}%` }} />
			</div>
			<div className={styles.legend}>
				<span>
					Disponibles {formatPercentage(available)} ({availableQuantity})
				</span>
				<span>
					Reservados {formatPercentage(reserved)} ({reservedQuantity})
				</span>
			</div>
		</>
	);
}
