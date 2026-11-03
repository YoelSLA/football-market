import {
	DEMO_DATA_LABEL,
	DEMO_MESSAGES,
	DEMO_QUOTE_UNIT_LABEL,
} from "@/features/players/constants";
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
 * Cotización actual del token de demostración con su variación derivada de las dos últimas
 * cotizaciones. Nunca se presenta como precio operativo ni valor de transferencia.
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
				<span>{DEMO_QUOTE_UNIT_LABEL}</span>
			</header>

			{loading ? (
				<p role="status">Preparando cotización de demostración…</p>
			) : !hasHistory ? (
				<p role="status">{DEMO_MESSAGES.emptySeries}</p>
			) : (
				<dl>
					<div>
						<dt>Precio</dt>
						<dd>
							{snapshot?.price} {DEMO_QUOTE_UNIT_LABEL}
						</dd>
					</div>
					<div>
						<dt>Fecha</dt>
						<dd>{snapshot?.date}</dd>
					</div>
					<div>
						<dt>Variación</dt>
						<dd>
							{variationKnown
								? `${snapshot?.absoluteChange} ${DEMO_QUOTE_UNIT_LABEL} (${DIRECTION_LABEL[(snapshot?.direction as keyof typeof DIRECTION_LABEL) ?? "unknown"]})`
								: DEMO_MESSAGES.singlePoint}
						</dd>
					</div>
					<div>
						<dt>Variación porcentual</dt>
						<dd>
							{snapshot?.percentageChange === null ||
							snapshot?.percentageChange === undefined
								? DEMO_MESSAGES.percentageUnavailable
								: `${snapshot.percentageChange.toFixed(2)} %`}
						</dd>
					</div>
				</dl>
			)}
			<p className={styles.playerQuotePanel__demo}>{DEMO_DATA_LABEL}</p>
		</section>
	);
}
