import { Link } from "react-router-dom";
import { getLeagueClassification } from "@/features/players/constants";
import type { Player } from "@/features/players/types";
import NationalityFlag from "./NationalityFlag";
import styles from "./PlayerCard.module.scss";
import PlayerPortrait from "./PlayerPortrait";
import TeamBadge from "./TeamBadge";

interface Props {
	player: Player;
}
const positions: Record<string, string> = {
	Goalkeeper: "POR",
	Defence: "DEF",
	Defender: "DEF",
	Midfield: "MED",
	Midfielder: "MED",
	Offence: "DEL",
	Forward: "DEL",
};
const number = new Intl.NumberFormat("es-AR", { maximumFractionDigits: 2 });

export default function PlayerCard({ player }: Props) {
	const league = getLeagueClassification(player.leagueName);
	const stats = player.statistics;
	const values = [
		{ label: "Partidos", value: stats?.appearancesTotal },
		{ label: "Goles", value: stats?.goalsTotal },
		{ label: "Asistencias", value: stats?.assistsTotal },
	];
	return (
		<article className={styles.playerCard}>
			<div className={styles.playerCard__visual}>
				<div className={styles.playerCard__identityPanel}>
					<span className={styles.playerCard__position} title={player.position}>
						{positions[player.position] ?? player.position}
					</span>
					<TeamBadge
						key={player.teamName}
						teamName={player.teamName}
						leagueName={player.leagueName}
					/>
					{league.image && (
						<img
							className={styles.playerCard__leagueIcon}
							src={league.image}
							alt={player.leagueName}
						/>
					)}
					<NationalityFlag
						key={player.nationality}
						nationality={player.nationality}
					/>
				</div>
				<PlayerPortrait
					key={`${player.id}:${player.imageUrl}:${player.fallbackImageUrl}`}
					player={player}
				/>
			</div>
			<header className={styles.playerCard__header}>
				<h2>
					<Link to={`/players/${player.id}`}>{player.name}</Link>
				</h2>
				<p>{player.teamName}</p>
				<div className={styles.playerCard__league}>
					{league.image && <img src={league.image} alt="" />}
					<span>{player.leagueName}</span>
				</div>
				<small>
					{player.nationality ?? "Nacionalidad no disponible"} · Activo
				</small>
			</header>
			<dl className={styles.playerCard__statistics}>
				{values.map((item) => (
					<div key={item.label}>
						<dd>
							{item.value == null ? "Sin datos" : number.format(item.value)}
						</dd>
						<dt>{item.label}</dt>
					</div>
				))}
			</dl>
			<p className={styles.playerCard__context}>
				{stats?.seasonLabel ?? stats?.seasonKey ?? "Temporada no disponible"}
				{stats?.extractionDate ? ` · ${stats.extractionDate}` : ""}
			</p>
		</article>
	);
}
