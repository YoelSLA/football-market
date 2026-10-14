import { DEMO_DATA_LABEL, DEMO_QUOTE_UNIT_LABEL } from "../../constants";
import type {
	PlayerDetail,
	PlayerSampleSeason,
	TokenQuoteSnapshot,
} from "../../types";
import { PlayerPortrait } from "../PlayerPortrait";
import styles from "./PlayerOverview.module.scss";

interface PlayerOverviewProps {
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
export function PlayerOverview({
	player,
	sampleSeason,
	quote,
	loadingQuotes,
	className,
}: PlayerOverviewProps) {
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
			className={`${styles["player-overview"]} ${className ?? ""}`.trim()}
			aria-label="Identidad del jugador"
		>
			<PlayerPortrait
				className={styles["player-overview__portrait"]}
				name={player.name}
				imageUrl={player.imageUrl}
				fallbackImageUrl={player.fallbackImageUrl}
			/>
			<div className={styles["player-overview__identity"]}>
				<h1>{player.name}</h1>
				<p className={styles["player-overview__team"]}>
					{player.teamName} · {player.leagueName}
				</p>
				<dl className={styles["player-overview__details"]}>
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
			<dl className={styles["player-overview__indicators"]}>
				{indicators.map((indicator) => (
					<div key={indicator.label}>
						<dt>{indicator.label}</dt>
						<dd>{indicator.value}</dd>
					</div>
				))}
				<p className={styles["player-overview__demo"]}>{DEMO_DATA_LABEL}</p>
			</dl>
			{loadingQuotes ? (
				<p className={styles["player-overview__demo"]} role="status">
					Calculando cotización de demostración…
				</p>
			) : null}
		</section>
	);
}
