import { getLeagueClassification } from "@/features/players/constants";
import type { PlayerDetail } from "@/features/players/types";
import { getStatisticsPositionLabel } from "@/features/players/utils";
import {
	NationalityFlag,
	PlayerPortrait,
	TeamBadge,
} from "@/shared/components";
import { Shield } from "@/shared/icons";
import styles from "./PlayerOverview.module.scss";

interface Props {
	player: PlayerDetail;
	seasonLabel: string | null;
	extractionDate: string | null;
	competitionName: string | null;
	sourcesLabel?: string | null;
}

function formatDate(dateStr: string | null): string {
	if (!dateStr) return "No disponible";
	const parts = dateStr.split("-");
	if (parts.length === 3) {
		const [year, month, day] = parts;
		return `${day}/${month}/${year}`;
	}
	return dateStr;
}

export default function PlayerOverview({
	player,
	seasonLabel,
	extractionDate,
	competitionName,
	sourcesLabel,
}: Props) {
	const league = getLeagueClassification(player.leagueName);
	const positionLabel = getStatisticsPositionLabel(player.position);
	const resolvedSeason = seasonLabel
		? `Temporada ${seasonLabel}`
		: "Temporada no disponible";
	const resolvedCompetition = competitionName ?? "Competición no disponible";
	const resolvedExtraction = formatDate(extractionDate);
	const resolvedBirth = formatDate(player.dateOfBirth);
	const resolvedSources =
		sourcesLabel ?? (extractionDate ? "JSON + Scraping" : "No disponible");

	return (
		<section
			className={styles.playerOverview}
			aria-label="Identidad y contexto del jugador"
		>
			<div className={styles.playerOverview__identityBlock}>
				<PlayerPortrait
					className={styles.playerOverview__portrait}
					name={player.name}
					src={player.imageUrl}
					fallbackSrc={player.fallbackImageUrl}
					fit="contain"
				/>
				<div className={styles.playerOverview__info}>
					<h1 className={styles.playerOverview__name}>{player.name}</h1>

					<div className={styles.playerOverview__teamLine}>
						<span className={styles.playerOverview__team}>
							<TeamBadge
								teamName={player.teamName}
								leagueName={player.leagueName}
							/>
							{player.teamName}
						</span>
						<span
							className={styles.playerOverview__separator}
							aria-hidden="true"
						>
							·
						</span>
						<span className={styles.playerOverview__league}>
							{league.image && <img src={league.image} alt="" />}
							{player.leagueName}
						</span>
					</div>

					<div className={styles.playerOverview__metaLine}>
						<span className={styles.playerOverview__metaItem}>
							<Shield size={16} aria-hidden="true" />
							<span>{positionLabel}</span>
						</span>
						<span
							className={styles.playerOverview__separator}
							aria-hidden="true"
						>
							·
						</span>
						<span className={styles.playerOverview__metaItem}>
							<NationalityFlag nationality={player.nationality} />
							<span>{player.nationality ?? "No disponible"}</span>
						</span>
						<span
							className={styles.playerOverview__separator}
							aria-hidden="true"
						>
							·
						</span>
						<span className={styles.playerOverview__metaItem}>
							<span>{resolvedBirth}</span>
						</span>
					</div>
				</div>
			</div>

			<div className={styles.playerOverview__contextBlock}>
				<div className={styles.playerOverview__seasonBadge}>
					{resolvedSeason}
				</div>

				<div className={styles.playerOverview__statsContext}>
					<h2 className={styles.playerOverview__contextHeading}>
						Contexto estadístico
					</h2>
					<dl className={styles.playerOverview__contextGrid}>
						<div className={styles.playerOverview__contextRow}>
							<dt>Competición:</dt>
							<dd>{resolvedCompetition}</dd>
						</div>
						<div className={styles.playerOverview__contextRow}>
							<dt>Actualizado:</dt>
							<dd>{resolvedExtraction}</dd>
						</div>
						<div className={styles.playerOverview__contextRow}>
							<dt>Fuentes:</dt>
							<dd>{resolvedSources}</dd>
						</div>
					</dl>
				</div>
			</div>
		</section>
	);
}
