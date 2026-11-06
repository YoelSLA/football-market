import type { PlayerRankingItem } from "@/features/players/types";
import { getNationalityFlag } from "@/features/players/utils";
import { NationalityFlag, TeamBadge } from "@/shared/components";
import styles from "./RankingPlaceMeta.module.scss";

interface Props {
	player: PlayerRankingItem;
}

/**
 * Resuelve la bandera correspondiente al país de la liga.
 */
function getLeagueCountryFlag(leagueName: string): string | null {
	const norm = leagueName.toLowerCase().trim();

	if (norm.includes("premier")) return getNationalityFlag("England");

	if (norm.includes("primera") || norm.includes("liga"))
		return getNationalityFlag("Spain");

	if (norm.includes("serie a")) return getNationalityFlag("Italy");

	if (norm.includes("bundesliga")) return getNationalityFlag("Germany");

	if (norm.includes("ligue")) return getNationalityFlag("France");

	return null;
}

export default function RankingPlaceMeta({ player }: Props) {
	const flag = getLeagueCountryFlag(player.leagueName) ?? player.leagueFlag;

	return (
		<div className={styles.meta}>
			<div className={styles.meta__clubRow}>
				<TeamBadge
					src={player.teamBadge}
					teamName={player.teamName}
					leagueName={player.leagueName}
					className={styles.meta__shield}
				/>
				<span className={styles.meta__teamName}>{player.teamName}</span>
			</div>

			<div className={styles.meta__leagueRow}>
				{flag && (
					<NationalityFlag
						src={flag}
						nationality={player.leagueName}
						className={styles.meta__flag}
					/>
				)}
				<span className={styles.meta__leagueName}>{player.leagueName}</span>
			</div>
		</div>
	);
}
