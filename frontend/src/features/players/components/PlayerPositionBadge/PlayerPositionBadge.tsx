import { POSITION_BADGES } from "@/features/players/constants";
import styles from "./PlayerPositionBadge.module.scss";

interface Props {
	position: string;
	className?: string;
}

export default function PlayerPositionBadge({ position, className }: Props) {
	const label = POSITION_BADGES[position] ?? position;
	const toneClass =
		label === "ARQ" || label === "POR"
			? styles.playerPositionBadge_goalkeeper
			: styles.playerPositionBadge_outfield;

	return (
		<span
			className={`${styles.playerPositionBadge} ${toneClass} ${className ?? ""}`.trim()}
			title={position}
		>
			{label}
		</span>
	);
}
