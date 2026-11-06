import { getLeagueClassification } from "@/features/players/constants";
import type { PlayerDetail } from "@/features/players/types";
import { getStatisticsPositionLabel } from "@/features/players/utils";
import {
	NationalityFlag,
	PlayerPortrait,
	TeamBadge,
} from "@/shared/components";
import { Activity, CalendarDays, Clock, Shield } from "@/shared/icons";
import styles from "./PlayerOverview.module.scss";

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
	return (
		<section
			className={styles.playerOverview}
			aria-label="Identidad del jugador"
		>
			<PlayerPortrait
				className={styles.playerOverview__portrait}
				name={player.name}
				src={player.imageUrl}
				fallbackSrc={player.fallbackImageUrl}
				fit="contain"
			/>
			<div className={styles.playerOverview__identity}>
				<h1>{player.name}</h1>
				<div className={styles.playerOverview__team}>
					<span>
						<TeamBadge
							teamName={player.teamName}
							leagueName={player.leagueName}
						/>
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
						<NationalityFlag nationality={player.nationality} />
						<span>
							<dd>{player.nationality ?? "No disponible"}</dd>
							<dt>Nacionalidad</dt>
						</span>
					</div>
				</dl>
			</div>
			<div className={styles.playerOverview__context}>
				<span className={styles.playerOverview__season}>
					Temporada {seasonLabel ?? "2024/2025"}
				</span>
				<dl>
					<div>
						<Clock size={20} aria-hidden="true" />
						<dt>Última actualización</dt>
						<dd>{extractionDate ?? "No disponible"}</dd>
					</div>
					<div>
						<Activity size={20} aria-hidden="true" />
						<dt>Competición de referencia</dt>
						<dd>{competitionName ?? player.leagueName}</dd>
					</div>
				</dl>
				<p>Datos sincronizados con las estadísticas oficiales.</p>
			</div>
		</section>
	);
}
