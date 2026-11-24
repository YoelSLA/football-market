import { portfolioLabels } from "@/features/portfolio/constants";
import { PlayerPortrait } from "@/shared/components";
import styles from "./PositionCardHeader.module.scss";

interface Props {
	imageUrl?: string | null;
	playerName?: string | null;
	teamName?: string | null;
	position?: string | null;
	status: "OPEN" | "CLOSED";
}

export default function PositionCardHeader({
	imageUrl,
	playerName,
	teamName,
	position,
	status,
}: Props) {
	const isOpen = status === "OPEN";

	return (
		<header className={styles.header}>
			<PlayerPortrait
				src={imageUrl}
				name={playerName}
				className={styles.portrait}
			/>
			<div className={styles.identity}>
				<h2>{playerName ?? portfolioLabels.missing}</h2>
				<div className={styles.meta}>
					<p>
						<span aria-hidden="true" className={styles.ball}>
							⚽&#xFE0E;
						</span>
						{teamName ?? portfolioLabels.missing}
					</p>
					<p>{position ?? portfolioLabels.missing}</p>
				</div>
			</div>
			<div className={styles.headerActions}>
				<span
					className={`${styles.status} ${
						isOpen ? styles.status_open : styles.status_closed
					}`}
				>
					<span className={styles.statusDot} aria-hidden="true" />
					{isOpen ? "ABIERTA" : "CERRADA"}
				</span>
			</div>
		</header>
	);
}
