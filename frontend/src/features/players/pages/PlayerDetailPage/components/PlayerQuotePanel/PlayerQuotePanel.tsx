import type { TokenQuoteSnapshot } from "@/features/players/types";
import { CREDIT_SYMBOL } from "@/shared/constants";
import { QuoteMetricItem } from "./components";
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

	const variationTone =
		snapshot?.direction === "up"
			? "positive"
			: snapshot?.direction === "down"
				? "negative"
				: "neutral";

	const percentageTone =
		snapshot?.percentageChange != null && snapshot.percentageChange !== 0
			? snapshot.percentageChange > 0
				? "positive"
				: "negative"
			: "neutral";

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
					<QuoteMetricItem
						label="Precio"
						value={`${snapshot?.price} ${CREDIT_SYMBOL}`}
					/>
					<QuoteMetricItem label="Fecha" value={snapshot?.date ?? "—"} />
					<QuoteMetricItem
						label="Variación"
						value={
							variationKnown
								? `${snapshot?.absoluteChange} ${CREDIT_SYMBOL} (${DIRECTION_LABEL[snapshot?.direction ?? "unknown"]})`
								: "Variación no disponible"
						}
						tone={variationKnown ? variationTone : "neutral"}
					/>
					<QuoteMetricItem
						label="Variación porcentual"
						value={
							snapshot?.percentageChange === null ||
							snapshot?.percentageChange === undefined
								? "Variación porcentual no disponible"
								: `${snapshot.percentageChange.toFixed(2)} %`
						}
						tone={percentageTone}
					/>
				</dl>
			)}
		</section>
	);
}
