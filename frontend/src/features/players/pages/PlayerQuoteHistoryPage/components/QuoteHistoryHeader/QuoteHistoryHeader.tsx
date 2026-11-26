import { Link } from "react-router-dom";
import type { PlayerDetail, PlayerQuote } from "@/features/players/types";
import {
	CompetitionLogo,
	NationalityFlag,
	PlayerPortrait,
	PlayerPositionBadge,
	TeamBadge,
} from "@/shared/components";
import { CREDIT_SYMBOL } from "@/shared/constants";
import { Activity, ArrowLeft, CalendarDays, TrendingUp } from "@/shared/icons";
import styles from "./QuoteHistoryHeader.module.scss";

interface Props {
	player: PlayerDetail;
	currentQuote?: PlayerQuote | null;
	allQuotesCount: number;
	latestQuote?: PlayerQuote | null;
	isLoading?: boolean;
}

export default function QuoteHistoryHeader({
	player,
	currentQuote,
	allQuotesCount,
	latestQuote,
	isLoading = false,
}: Props) {
	const latestPct = latestQuote?.percentageChange;
	const pctFormatted =
		latestPct !== null && latestPct !== undefined
			? `${latestPct >= 0 ? "+" : ""}${latestPct.toFixed(2).replace(".", ",")} %`
			: "—";

	return (
		<header className={styles.header}>
			<div className={styles.header__left}>
				<Link
					to={`/players/${player.id}`}
					className={styles.header__avatarLink}
					title={`Volver a la ficha de ${player.name}`}
				>
					<PlayerPortrait
						className={styles.header__portrait}
						name={player.name}
						src={player.imageUrl}
						fallbackSrc={player.fallbackImageUrl}
						fit="contain"
					/>
				</Link>

				<div className={styles.header__titleGroup}>
					<Link
						to={`/players/${player.id}`}
						className={styles.header__backLink}
					>
						<ArrowLeft size={12} aria-hidden="true" />
						<span>Volver a la ficha</span>
					</Link>

					<div className={styles.header__nameRow}>
						<h1 className={styles.header__title}>{player.name}</h1>
						<PlayerPositionBadge
							position={player.position}
							className={styles.header__positionBadge}
						/>
					</div>

					<div className={styles.header__metaRow}>
						{player.teamName && (
							<span className={styles.header__metaItem}>
								<TeamBadge
									teamName={player.teamName}
									leagueName={player.leagueName}
								/>
								<span>{player.teamName}</span>
							</span>
						)}

						{player.teamName && player.leagueName && (
							<span className={styles.header__metaDivider}>•</span>
						)}

						{player.leagueName && (
							<span className={styles.header__metaItem}>
								<CompetitionLogo
									competitionName={player.leagueName}
									decorative
								/>
								<span>{player.leagueName}</span>
							</span>
						)}

						{player.nationality && (
							<>
								<span className={styles.header__metaDivider}>•</span>
								<span className={styles.header__metaItem}>
									<NationalityFlag nationality={player.nationality} />
									<span>{player.nationality}</span>
								</span>
							</>
						)}
					</div>
				</div>
			</div>

			<section
				aria-label="Indicadores de cotizaciones"
				className={styles.header__metrics}
			>
				{/* Métrica 1: Cotización actual */}
				<div
					className={`${styles.header__metricItem} ${styles.header__metric_blue}`}
					title="Cotización oficial vigente del jugador"
				>
					<span className={styles.header__metricIconWrapper} aria-hidden="true">
						<TrendingUp className={styles.header__metricIcon} />
					</span>
					<div className={styles.header__metricBody}>
						<span className={styles.header__metricLabel}>
							Cotización Actual
						</span>
						<strong className={styles.header__metricValue}>
							{isLoading
								? "—"
								: currentQuote
									? `${currentQuote.price.toFixed(2).replace(".", ",")} ${CREDIT_SYMBOL}`
									: "—"}
						</strong>
						<span className={styles.header__metricDetail}>
							Precio oficial de referencia
						</span>
					</div>
				</div>

				{/* Métrica 2: Sesiones oficiales */}
				<div
					className={`${styles.header__metricItem} ${styles.header__metric_violet}`}
					title="Total de recálculos de cotizaciones registrados para el jugador"
				>
					<span className={styles.header__metricIconWrapper} aria-hidden="true">
						<CalendarDays className={styles.header__metricIcon} />
					</span>
					<div className={styles.header__metricBody}>
						<span className={styles.header__metricLabel}>
							Sesiones Oficiales
						</span>
						<strong className={styles.header__metricValue}>
							{isLoading ? "—" : `${allQuotesCount} registros`}
						</strong>
						<span className={styles.header__metricDetail}>
							Historial acumulado
						</span>
					</div>
				</div>

				{/* Métrica 3: Última variación */}
				<div
					className={`${styles.header__metricItem} ${styles.header__metric_indigo}`}
					title="Variación porcentual en la última sesión respecto a la anterior"
				>
					<span className={styles.header__metricIconWrapper} aria-hidden="true">
						<Activity className={styles.header__metricIcon} />
					</span>
					<div className={styles.header__metricBody}>
						<span className={styles.header__metricLabel}>Última Variación</span>
						<strong className={styles.header__metricValue}>
							{isLoading ? "—" : pctFormatted}
						</strong>
						<span className={styles.header__metricDetail}>
							Respecto a sesión previa
						</span>
					</div>
				</div>
			</section>
		</header>
	);
}
