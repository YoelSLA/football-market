import {
	DEMO_DATA_LABEL,
	DEMO_MESSAGES,
	DEMO_QUOTE_UNIT_LABEL,
} from "../../constants";
import type { TokenQuoteSnapshot } from "../../types";
import styles from "./PlayerQuotePanel.module.scss";

interface PlayerQuotePanelProps {
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
export function PlayerQuotePanel({
	snapshot,
	loading,
	className,
}: PlayerQuotePanelProps) {
	const hasHistory = snapshot?.price !== null && snapshot !== undefined;
	const variationKnown =
		snapshot?.absoluteChange !== null && snapshot?.absoluteChange !== undefined;

	return (
		<section
			className={`${styles["player-quote-panel"]} ${className ?? ""}`.trim()}
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
								? `${snapshot?.absoluteChange} ${DEMO_QUOTE_UNIT_LABEL} (${DIRECTION_LABEL[snapshot?.direction ?? "unknown"]})`
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

			<p className={styles["player-quote-panel__demo"]}>{DEMO_DATA_LABEL}</p>
		</section>
	);
}
