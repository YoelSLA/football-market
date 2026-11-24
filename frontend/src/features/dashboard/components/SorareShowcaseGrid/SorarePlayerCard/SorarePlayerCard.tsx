import { Link } from "react-router-dom";
import {
	SorareCardFooter,
	SorareCardHeader,
	SorareCardMedia,
} from "./components";
import styles from "./SorarePlayerCard.module.scss";

export interface SorarePlayerCardProps {
	playerId: number;
	name: string;
	team: string;
	league: string;
	position: string;
	imageUrl?: string | null;
	score?: number | null;
	price: number;
	deltaPercentage?: number | null;
	tokensOwned?: number;
	valueRatio?: number;
	className?: string;
}

export function SorarePlayerCard({
	playerId,
	name,
	team,
	league,
	position,
	imageUrl,
	score,
	price,
	deltaPercentage,
	tokensOwned,
	valueRatio,
	className,
}: SorarePlayerCardProps) {
	return (
		<article
			className={`${styles.card} ${className ?? ""}`.trim()}
			aria-label={`Carta coleccionable de ${name}`}
		>
			<div className={styles.cardGlow} aria-hidden="true" />

			<SorareCardHeader position={position} score={score} />

			<SorareCardMedia
				imageUrl={imageUrl}
				name={name}
				team={team}
				league={league}
			/>

			<div className={styles.infoSection}>
				<h3 className={styles.playerName} title={name}>
					{name}
				</h3>
				<span className={styles.teamName}>{team}</span>
			</div>

			<SorareCardFooter
				price={price}
				tokensOwned={tokensOwned}
				deltaPercentage={deltaPercentage}
				valueRatio={valueRatio}
			/>

			<Link
				to={`/players/${playerId}`}
				className={styles.cardActionLink}
				aria-label={`Ver perfil de ${name}`}
			>
				Explorar Activo →
			</Link>
		</article>
	);
}
