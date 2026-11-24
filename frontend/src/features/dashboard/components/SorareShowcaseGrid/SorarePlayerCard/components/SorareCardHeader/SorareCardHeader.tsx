import { Star } from "@/shared/icons";
import styles from "./SorareCardHeader.module.scss";

interface Props {
	position: string;
	score?: number | null;
}

export default function SorareCardHeader({ position, score }: Props) {
	return (
		<header className={styles.header}>
			<span className={styles.positionBadge}>{position}</span>

			{score !== undefined && score !== null && (
				<span className={styles.scoreBadge} title="Score WhoScored">
					<Star size={11} className={styles.starIcon} aria-hidden="true" />
					<span>{score.toFixed(2)}</span>
				</span>
			)}
		</header>
	);
}
