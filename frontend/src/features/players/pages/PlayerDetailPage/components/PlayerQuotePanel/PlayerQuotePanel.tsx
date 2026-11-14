import type { TokenQuoteSnapshot } from "@/features/players/types";
import styles from "./PlayerQuotePanel.module.scss";

interface Props {
	snapshot?: TokenQuoteSnapshot;
	loading?: boolean;
	className?: string;
}

const DIRECTION_LABEL = {
	up: "Sube",
	down: "Baja",
	flat: "Estable",
	unknown: "Sin variación",
} as const;

/**
 * Cotización oficial actual del token derivada del historial persistido.
 */
export default function PlayerQuotePanel({
	snapshot,
	loading,
	className,
}: Props) {
	const hasHistory = snapshot?.price !== null && snapshot !== undefined;
	const variationKnown =
		snapshot?.absoluteChange !== null && snapshot?.absoluteChange !== undefined;

	return (
		<section
			className={`${styles.playerQuotePanel} ${className ?? ""}`.trim()}
			aria-label="Cotización del token"
		>
			<header>
				<h2>Cotización del token</h2>
				<span>Créditos</span>
			</header>

			{loading ? (
				<p role="status">Cargando cotización…</p>
			) : !hasHistory ? (
				<p role="status">El jugador aún no tiene cotizaciones oficiales.</p>
			) : (
				<dl>
					<div>
						<dt>Precio</dt>
						<dd>{snapshot?.price} cr.</dd>
					</div>
					<div>
						<dt>Fecha</dt>
						<dd>{snapshot?.date}</dd>
					</div>
					<div>
						<dt>Variación</dt>
						<dd>
							{variationKnown
								? `${snapshot?.absoluteChange} cr. (${DIRECTION_LABEL[(snapshot?.direction as keyof typeof DIRECTION_LABEL) ?? "unknown"]})`
								: "Variación no disponible"}
						</dd>
					</div>
					<div>
						<dt>Variación porcentual</dt>
						<dd>
							{snapshot?.percentageChange === null ||
							snapshot?.percentageChange === undefined
								? "Variación porcentual no disponible"
								: `${snapshot.percentageChange.toFixed(2)} %`}
						</dd>
					</div>
				</dl>
			)}
		</section>
	);
}
