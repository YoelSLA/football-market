import { POSITION_BADGES } from "@/shared/constants";
import styles from "./PlayerPositionBadge.module.scss";

export interface PlayerPositionBadgeProps {
	position: string;
	className?: string;
}

export default function PlayerPositionBadge({
	position,
	className,
}: PlayerPositionBadgeProps) {
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
