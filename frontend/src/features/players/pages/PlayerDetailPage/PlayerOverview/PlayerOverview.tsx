import { getLeagueClassification } from "@/features/players/constants";
import type { PlayerDetail } from "@/features/players/types";
import {
	getNationalityFlag,
	getStatisticsPositionLabel,
} from "@/features/players/utils";
import { Activity, CalendarDays, Clock, Globe, Shield } from "@/shared/icons";
import { getTeamShield } from "@/shared/utils/teamShield";
import styles from "./PlayerOverview.module.scss";
import PlayerPortrait from "./PlayerPortrait";

interface Props {
	player: PlayerDetail;
	seasonLabel: string | null;
	extractionDate: string | null;
	competitionName: string | null;
}
export default function PlayerOverview({
	player,
	seasonLabel,
	extractionDate,
	competitionName,
}: Props) {
	const league = getLeagueClassification(player.leagueName);
	const teamLogo = getTeamShield(player.leagueName, player.teamName);
	const flag = getNationalityFlag(player.nationality);
	return (
		<section
			className={styles.playerOverview}
			aria-label="Identidad del jugador"
		>
			<PlayerPortrait
				className={styles.playerOverview__portrait}
				name={player.name}
				imageUrl={player.imageUrl}
				fallbackImageUrl={player.fallbackImageUrl}
			/>
			<div className={styles.playerOverview__identity}>
				<h1>{player.name}</h1>
				<div className={styles.playerOverview__team}>
					<span>
						{teamLogo && (
							<img
								src={teamLogo}
								alt=""
								onError={(event) => {
									event.currentTarget.hidden = true;
								}}
							/>
						)}
						{player.teamName}
					</span>
					<span>
						{league.image && <img src={league.image} alt="" />}
						{player.leagueName}
					</span>
				</div>
				<dl className={styles.playerOverview__details}>
					<div>
						<Shield size={26} aria-hidden="true" />
						<span>
							<dd>{getStatisticsPositionLabel(player.position)}</dd>
							<dt>Posición</dt>
						</span>
					</div>
					<div>
						<CalendarDays size={26} aria-hidden="true" />
						<span>
							<dd>{player.dateOfBirth ?? "No disponible"}</dd>
							<dt>Fecha de nacimiento</dt>
						</span>
					</div>
					<div>
						{flag ? (
							<img
								src={flag}
								alt=""
								onError={(event) => {
									event.currentTarget.hidden = true;
								}}
							/>
						) : (
							<Globe size={24} aria-hidden="true" />
						)}
						<span>
							<dd>{player.nationality ?? "No disponible"}</dd>
							<dt>Nacionalidad</dt>
						</span>
					</div>
				</dl>
			</div>
			<div className={styles.playerOverview__context}>
				<span className={styles.playerOverview__season}>
					Temporada {seasonLabel ?? "no disponible"}
				</span>
				<dl>
					<div>
						<Clock size={22} aria-hidden="true" />
						<dt>Última extracción</dt>
						<dd>{extractionDate ?? "Sin datos"}</dd>
					</div>
					<div>
						<Activity size={22} aria-hidden="true" />
						<dt>Competición estadística</dt>
						<dd>{competitionName ?? "Competición no disponible"}</dd>
					</div>
				</dl>
				<p>Rendimiento de temporada · estadísticas reales de WhoScored</p>
			</div>
		</section>
	);
}
