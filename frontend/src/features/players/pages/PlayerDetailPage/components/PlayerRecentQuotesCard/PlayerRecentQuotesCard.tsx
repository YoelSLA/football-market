import { useNavigate } from "react-router-dom";
import type { PlayerQuoteHistory } from "@/features/players/types";
import { CREDIT_SYMBOL } from "@/shared/constants";
import { ArrowRight, History } from "@/shared/icons";
import styles from "./PlayerRecentQuotesCard.module.scss";

interface Props {
	quotes?: PlayerQuoteHistory;
	playerId: number;
}

function formatShortDate(dateStr: string): string {
	if (!dateStr) return "—";
	const parts = dateStr.slice(0, 10).split("-");
	if (parts.length < 3) return dateStr;
	return `${parts[2]}/${parts[1]}`;
}

export default function PlayerRecentQuotesCard({ quotes, playerId }: Props) {
	const navigate = useNavigate();
	const allQuotes = quotes?.quotes ?? [];
	const MAX_PREVIEW_QUOTES = 10;
	const recentQuotes = allQuotes.slice(0, MAX_PREVIEW_QUOTES);

	const handleGoToHistory = () => {
		navigate(`/players/${playerId}/history`);
	};

	return (
		<section
			className={styles.recentQuotesCard}
			aria-label="Historial de cotizaciones oficiales"
		>
			<header className={styles.recentQuotesCard__header}>
				<div className={styles.recentQuotesCard__titleRow}>
					<div className={styles.recentQuotesCard__iconWrapper}>
						<History size={16} aria-hidden="true" />
					</div>
					<div>
						<h3 className={styles.recentQuotesCard__title}>
							Historial de Cotizaciones
						</h3>
						<span className={styles.recentQuotesCard__subtitle}>
							{allQuotes.length > 0
								? allQuotes.length <= MAX_PREVIEW_QUOTES
									? `${allQuotes.length} sesiones registradas`
									: `Mostrando ${recentQuotes.length} de ${allQuotes.length} sesiones`
								: "Registro oficial"}
						</span>
					</div>
				</div>

				<button
					type="button"
					className={styles.recentQuotesCard__headerBtn}
					onClick={handleGoToHistory}
				>
					<span>Ver Completo</span>
					<ArrowRight size={13} aria-hidden="true" />
				</button>
			</header>

			<div className={styles.recentQuotesCard__tableContainer}>
				{recentQuotes.length === 0 ? (
					<p className={styles.recentQuotesCard__empty} role="status">
						Aún no hay cotizaciones históricas registradas.
					</p>
				) : (
					<table className={styles.recentQuotesCard__table}>
						<thead>
							<tr>
								<th>Fecha</th>
								<th>Cotización</th>
								<th>Variación</th>
								<th>Deporte</th>
								<th>Mercado</th>
							</tr>
						</thead>
						<tbody>
							{recentQuotes.map((q, idx) => {
								const isLatest = idx === 0;
								const pct = q.percentageChange ?? 0;
								const isUp = q.direction === "up" || pct > 0;
								const isDown = q.direction === "down" || pct < 0;

								const sportMult = q.breakdown?.sportMultiplier ?? 1.12;
								const marketMult = q.breakdown?.marketMultiplier ?? 1.04;

								return (
									<tr key={q.effectiveDate + q.calculatedAt}>
										<td className={styles.recentQuotesCard__cellDate}>
											{isLatest && (
												<span
													className={styles.recentQuotesCard__dot}
													aria-hidden="true"
												/>
											)}
											<span>{formatShortDate(q.effectiveDate)}</span>
											{isLatest && (
												<span className={styles.recentQuotesCard__tagHoy}>
													(Hoy)
												</span>
											)}
										</td>
										<td className={styles.recentQuotesCard__cellPrice}>
											<strong>{q.price.toFixed(2).replace(".", ",")}</strong>{" "}
											{CREDIT_SYMBOL}
										</td>
										<td>
											<span
												className={`${styles.recentQuotesCard__badge} ${
													isUp
														? styles["recentQuotesCard__badge--up"]
														: isDown
															? styles["recentQuotesCard__badge--down"]
															: styles["recentQuotesCard__badge--flat"]
												}`}
											>
												{isUp
													? `▲ +${pct.toFixed(2)} %`
													: isDown
														? `▼ ${pct.toFixed(2)} %`
														: "● 0,00 %"}
											</span>
										</td>
										<td className={styles.recentQuotesCard__cellFactorSport}>
											x{sportMult.toFixed(2)}
										</td>
										<td className={styles.recentQuotesCard__cellFactorMarket}>
											x{marketMult.toFixed(2)}
										</td>
									</tr>
								);
							})}
						</tbody>
					</table>
				)}
			</div>

			<footer className={styles.recentQuotesCard__footer}>
				<span className={styles.recentQuotesCard__footerInfo}>
					Auditoría completa de variaciones y emisión oficial
				</span>
				<button
					type="button"
					className={styles.recentQuotesCard__footerBtn}
					onClick={handleGoToHistory}
				>
					<span>Ir a Historial Completo ({allQuotes.length})</span>
					<ArrowRight size={14} aria-hidden="true" />
				</button>
			</footer>
		</section>
	);
}
