import { PlayerPortrait, TeamBadge } from "@/shared/components";
import styles from "./SorareCardMedia.module.scss";

interface Props {
	imageUrl?: string | null;
	name: string;
	team: string;
	league: string;
}

export default function SorareCardMedia({
	imageUrl,
	name,
	team,
	league,
}: Props) {
	return (
		<div className={styles.portraitSection}>
			<PlayerPortrait
				src={imageUrl}
				name={name}
				size="lg"
				className={styles.portrait}
			/>
			<div className={styles.badgeOverlay}>
				<TeamBadge
					teamName={team}
					leagueName={league}
					className={styles.teamShield}
				/>
			</div>
		</div>
	);
}
