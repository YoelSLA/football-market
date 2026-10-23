import {
	DEMO_DATA_LABEL,
	DEMO_QUOTE_UNIT_LABEL,
} from "@/features/players/constants";
import type {
	PlayerDetail,
	PlayerSampleSeason,
	TokenQuoteSnapshot,
} from "@/features/players/types";
import styles from "./PlayerOverview.module.scss";
import PlayerPortrait from "./PlayerPortrait";

interface Props {
	player: PlayerDetail;
	sampleSeason?: PlayerSampleSeason;
	quote?: TokenQuoteSnapshot;
	loadingQuotes?: boolean;
	className?: string;
}

const NOT_AVAILABLE = "No disponible";

function formatDate(value: string | null): string {
	return value ? value : NOT_AVAILABLE;
}

/**
 * Ficha principal del detalle: identidad real dominante y franja de indicadores con el
 * resumen deportivo y la cotización de demostración. La identidad nunca se etiqueta como
 * simulada; los indicadores sí identifican su procedencia.
 */
export default function PlayerOverview({
	player,
	sampleSeason,
	quote,
	loadingQuotes,
	className,
}: Props) {
	const indicators = [
		{
			label: "Partidos",
			value: sampleSeason ? `${sampleSeason.matches}` : "—",
		},
		{ label: "Goles", value: sampleSeason ? `${sampleSeason.goals}` : "—" },
		{
			label: "Asistencias",
			value: sampleSeason ? `${sampleSeason.assists}` : "—",
		},
		{
			label: "Cotización",
			value:
				quote?.price === null || quote === undefined
					? "—"
					: `${quote.price} ${DEMO_QUOTE_UNIT_LABEL}`,
		},
	];

	return (
		<section
			className={`${styles.playerOverview} ${className ?? ""}`.trim()}
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
				<p className={styles.playerOverview__team}>
					{player.teamName} · {player.leagueName}
				</p>
				<dl className={styles.playerOverview__details}>
					<div>
						<dt>Posición</dt>
						<dd>{player.position || NOT_AVAILABLE}</dd>
					</div>
					<div>
						<dt>Fecha de nacimiento</dt>
						<dd>{formatDate(player.dateOfBirth)}</dd>
					</div>
					<div>
						<dt>Nacionalidad</dt>
						<dd>{formatDate(player.nationality)}</dd>
					</div>
				</dl>
			</div>
			<dl className={styles.playerOverview__indicators}>
				{indicators.map((indicator) => (
					<div key={indicator.label}>
						<dt>{indicator.label}</dt>
						<dd>{indicator.value}</dd>
					</div>
				))}
				<p className={styles.playerOverview__demo}>{DEMO_DATA_LABEL}</p>
			</dl>
			{loadingQuotes ? (
				<p className={styles.playerOverview__demo} role="status">
					Calculando cotización de demostración…
				</p>
			) : null}
		</section>
	);
}
