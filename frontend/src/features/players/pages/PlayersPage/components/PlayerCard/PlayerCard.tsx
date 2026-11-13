import { Link, useLocation } from "react-router-dom";
import { getLeagueClassification } from "@/features/players/constants";
import type { Player } from "@/features/players/types";
import {
	CompetitionLogo,
	NationalityFlag,
	PlayerPortrait,
	TeamBadge,
} from "@/shared/components";
import styles from "./PlayerCard.module.scss";

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
	const location = useLocation();
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
					<CompetitionLogo
						competitionName={player.leagueName}
						src={league.image}
						className={styles.playerCard__leagueIcon}
					/>
					<NationalityFlag
						key={player.nationality}
						nationality={player.nationality}
					/>
				</div>
				<PlayerPortrait
					key={`${player.id}:${player.imageUrl}:${player.fallbackImageUrl}`}
					src={player.imageUrl}
					fallbackSrc={player.fallbackImageUrl}
					name={player.name}
					alt=""
					fit="contain"
					className={styles.playerCard__portrait}
				/>
			</div>
			<header className={styles.playerCard__header}>
				<h2>
					<Link
						to={`/players/${player.id}`}
						state={{ from: `${location.pathname}${location.search}` }}
					>
						{player.name}
					</Link>
				</h2>
				<p>{player.teamName}</p>
				<div className={styles.playerCard__league}>
					<CompetitionLogo
						competitionName={player.leagueName}
						src={league.image}
						decorative
					/>
					<span>{player.leagueName}</span>
				</div>
				<small>
					{player.nationality ?? "Nacionalidad no disponible"} · Activo
				</small>
			</header>
			<dl className={styles.playerCard__statistics}>
				{values.map((item) => (
					<div key={item.label}>
						<dt>{item.label}</dt>
						<dd>
							{item.value !== null && item.value !== undefined
								? number.format(item.value)
								: "—"}
						</dd>
					</div>
				))}
			</dl>
			<p className={styles.playerCard__context}>
				Estadísticas oficiales consolidadas
			</p>
		</article>
	);
}
