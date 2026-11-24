import { formatCredits } from "@/features/portfolio/utils";
import { ArrowRight, Check } from "@/shared/icons";
import styles from "./PositionCardFooter.module.scss";

interface Props {
	result: number | null;
	playerName?: string | null;
	playerId: number | string;
	onDetails: () => void;
}

export default function PositionCardFooter({
	result,
	playerName,
	playerId,
	onDetails,
}: Props) {
	const resultTone =
		result === null || result === 0
			? "neutral"
			: result > 0
				? "positive"
				: "negative";

	const resultToneClass = {
		positive: styles.result_positive,
		negative: styles.result_negative,
		neutral: styles.result_neutral,
	}[resultTone];

	return (
		<footer className={styles.footer}>
			<span className={`${styles.result} ${resultToneClass}`}>
				<Check
					className={styles.resultIcon}
					aria-hidden="true"
					size={16}
					strokeWidth={2}
				/>
				<span className={styles.resultValue}>
					Resultado: {formatCredits(result, true)}
				</span>
			</span>
			<button
				type="button"
				className={styles.details}
				onClick={onDetails}
				aria-label={`Ver detalles de ${playerName ?? `jugador ${playerId}`}`}
			>
				Ver detalles
				<ArrowRight className={styles.arrowIcon} aria-hidden="true" size={16} />
			</button>
		</footer>
	);
}
