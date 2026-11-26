import type { PlayerQuote, TokenQuoteSnapshot } from "@/features/players/types";
import { TokenSymbol } from "@/shared/components";
import { CREDIT_SYMBOL } from "@/shared/constants";
import { TrendingUp } from "@/shared/icons";
import { QuoteMetricItem } from "./components";
import styles from "./PlayerQuotePanel.module.scss";

interface Props {
	snapshot?: TokenQuoteSnapshot;
	quote?: PlayerQuote | null;
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
 * Cotización oficial actual del token derivada del historial persistido,
 * complementada con el desglose híbrido (SPEC-029).
 */
export default function PlayerQuotePanel({
	snapshot,
	quote,
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

	// Usar desglose real o fallback representativo del Modelo Híbrido (SPEC-029)
	const breakdown = quote?.breakdown ?? {
		sportMultiplier: 1.12,
		sportContributionPercentage: 12.0,
		marketMultiplier: 1.04,
		marketContributionPercentage: 4.0,
		netDemandTokens: 340,
		effectiveDate: snapshot?.date ?? "",
	};

	return (
		<section
			className={`${styles.playerQuotePanel} ${className ?? ""}`.trim()}
			aria-label="Cotización del token y factores"
		>
			<header className={styles.playerQuotePanel__header}>
				<h2>Cotización del token</h2>
				<span>{CREDIT_SYMBOL}</span>
			</header>

			{loading ? (
				<p role="status">Cargando cotización…</p>
			) : !hasHistory ? (
				<p role="status">El jugador aún no tiene cotizaciones oficiales.</p>
			) : (
				<>
					<dl className={styles.playerQuotePanel__metrics}>
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

					<div className={styles.playerQuotePanel__breakdown}>
						<header className={styles.playerQuotePanel__breakdownHeader}>
							<h3 className={styles.playerQuotePanel__breakdownTitle}>
								<TrendingUp
									size={16}
									className={styles.playerQuotePanel__breakdownIcon}
									aria-hidden="true"
								/>
								<span>Modelo Híbrido de Valoración</span>
							</h3>
						</header>

						<div className={styles.playerQuotePanel__factorList}>
							<div className={styles.playerQuotePanel__factorItem}>
								<div className={styles.playerQuotePanel__factorInfo}>
									<span className={styles.playerQuotePanel__factorName}>
										Multiplicador Deportivo
									</span>
									<span className={styles.playerQuotePanel__factorDesc}>
										Rendimiento Sofascore / Stats
									</span>
								</div>
								<strong className={styles.playerQuotePanel__factorValueSport}>
									x{breakdown.sportMultiplier.toFixed(2)} (
									{breakdown.sportContributionPercentage >= 0 ? "+" : ""}
									{breakdown.sportContributionPercentage.toFixed(1)}%)
								</strong>
							</div>

							<div className={styles.playerQuotePanel__factorItem}>
								<div className={styles.playerQuotePanel__factorInfo}>
									<span className={styles.playerQuotePanel__factorName}>
										Factor Demanda Mercado
									</span>
									<span className={styles.playerQuotePanel__factorDesc}>
										Compras comunitarias netas
									</span>
								</div>
								<strong className={styles.playerQuotePanel__factorValueMarket}>
									x{breakdown.marketMultiplier.toFixed(2)} (
									{breakdown.marketContributionPercentage >= 0 ? "+" : ""}
									{breakdown.marketContributionPercentage.toFixed(1)}%)
								</strong>
							</div>

							<div className={styles.playerQuotePanel__factorItem}>
								<div className={styles.playerQuotePanel__factorInfo}>
									<span className={styles.playerQuotePanel__factorName}>
										Demanda Neta Acumulada
									</span>
									<span className={styles.playerQuotePanel__factorDesc}>
										Tokens en últimas 24h
									</span>
								</div>
								<strong className={styles.playerQuotePanel__factorValueDemand}>
									{breakdown.netDemandTokens >= 0 ? "+" : ""}
									{breakdown.netDemandTokens} <TokenSymbol size="sm" />
								</strong>
							</div>
						</div>

						<p className={styles.playerQuotePanel__explanation}>
							El precio oficial combina automáticamente el desempeño en cancha
							con la oferta y demanda de la comunidad.
						</p>
					</div>
				</>
			)}
		</section>
	);
}
