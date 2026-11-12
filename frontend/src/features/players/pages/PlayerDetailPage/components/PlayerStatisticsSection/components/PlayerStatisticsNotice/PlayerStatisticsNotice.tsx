import type { ReactNode } from "react";
import styles from "./PlayerStatisticsNotice.module.scss";

interface Props {
	title: string;
	description: ReactNode;
	className?: string;
}

export default function PlayerStatisticsNotice({
	title,
	description,
	className,
}: Props) {
	return (
		<div className={`${styles.notice} ${className ?? ""}`.trim()} role="status">
			<strong className={styles.notice__title}>{title}</strong>
			<p className={styles.notice__description}>{description}</p>
		</div>
	);
}
