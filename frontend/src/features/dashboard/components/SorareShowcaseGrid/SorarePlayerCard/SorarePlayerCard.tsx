import { Link } from "react-router-dom";
import { PlayerPortrait, TeamBadge } from "@/shared/components";
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
	const formatPrice = (amount: number) => {
		return `${amount.toLocaleString("es-ES", {
			minimumFractionDigits: 2,
			maximumFractionDigits: 2,
		})} ₢`;
	};

	const formatDelta = (pct: number | null) => {
		if (pct === null) return null;
		const sign = pct > 0 ? "+" : "";
		return `${sign}${pct.toFixed(2)}%`;
	};

	const isPositive = (deltaPercentage ?? 0) >= 0;

	return (
		<article
			className={`${styles.card} ${className ?? ""}`.trim()}
			aria-label={`Carta coleccionable de ${name}`}
		>
			<div className={styles.cardGlow} aria-hidden="true" />

			{/* Cabecera de la carta: Posición y Score */}
			<header className={styles.cardHeader}>
				<span className={styles.positionBadge}>{position}</span>

				{score !== undefined && score !== null && (
					<span className={styles.scoreBadge} title="Score WhoScored">
						★ {score.toFixed(2)}
					</span>
				)}
			</header>

			{/* Retrato y Escudo */}
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

			{/* Información del futbolista */}
			<div className={styles.infoSection}>
				<h3 className={styles.playerName} title={name}>
					{name}
				</h3>
				<span className={styles.teamName}>{team}</span>
			</div>

			{/* Métricas financieras y ratios */}
			<footer className={styles.cardFooter}>
				<div className={styles.priceBlock}>
					<span className={styles.priceLabel}>Cotización</span>
					<span className={styles.priceValue}>{formatPrice(price)}</span>
				</div>

				<div className={styles.metaBlock}>
					{tokensOwned !== undefined && (
						<span className={styles.tokensBadge}>
							{tokensOwned} {tokensOwned === 1 ? "token" : "tokens"}
						</span>
					)}

					{deltaPercentage !== undefined && deltaPercentage !== null && (
						<span
							className={`${styles.deltaBadge} ${isPositive ? styles.deltaUp : styles.deltaDown}`}
						>
							{formatDelta(deltaPercentage)}
						</span>
					)}

					{valueRatio !== undefined && (
						<span className={styles.ratioBadge} title="Score por crédito">
							{valueRatio.toFixed(1)} pts/k₢
						</span>
					)}
				</div>
			</footer>

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
