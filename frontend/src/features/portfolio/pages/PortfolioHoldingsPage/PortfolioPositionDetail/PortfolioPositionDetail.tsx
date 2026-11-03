import { useEffect, useRef } from "react";
import { portfolioLabels } from "@/features/portfolio/constants";
import styles from "@/features/portfolio/pages/PortfolioHoldingsPage/PortfolioPositionDetail/PortfolioPositionDetail.module.scss";
import type { PortfolioPosition } from "@/features/portfolio/types";
import {
	formatCredits,
	formatDateTime,
	formatPercentage,
} from "@/features/portfolio/utils";
import {
	Activity,
	BarChart3,
	Coins,
	FileText,
	Package,
	TrendingUp,
} from "@/shared/icons";
import PortfolioPortrait from "../PortfolioPortrait";
import DetailPanel from "./DetailPanel";

interface Props {
	position: PortfolioPosition;
	onClose: () => void;
}

export default function PortfolioPositionDetail({
	position: p,
	onClose,
}: Props) {
	const dialogRef = useRef<HTMLDialogElement>(null);
	const closeRef = useRef<HTMLButtonElement>(null);

	useEffect(() => {
		const dialog = dialogRef.current;
		const opener = document.activeElement;
		const previousOverflow = document.body.style.overflow;
		dialog?.showModal();
		closeRef.current?.focus();
		document.body.style.overflow = "hidden";
		return () => {
			dialog?.close();
			document.body.style.overflow = previousOverflow;
			if (opener instanceof HTMLElement && opener.isConnected) opener.focus();
		};
	}, []);

	const quote =
		p.valuationStatus === "UNVALUED"
			? "Sin cotización"
			: formatCredits(p.currentUnitPrice, true);
	const available =
		p.quantity > 0 ? (p.availableQuantity / p.quantity) * 100 : 0;
	const reserved = p.quantity > 0 ? (p.reservedQuantity / p.quantity) * 100 : 0;
	const resultClass =
		p.totalProfitLoss == null || p.totalProfitLoss === 0
			? styles.portfolioDetail__neutral
			: p.totalProfitLoss > 0
				? styles.portfolioDetail__positive
				: styles.portfolioDetail__negative;

	return (
		<dialog
			ref={dialogRef}
			className={styles.portfolioDetail}
			aria-labelledby="portfolio-detail-title"
			aria-describedby="portfolio-detail-description"
			onCancel={(event) => {
				event.preventDefault();
				onClose();
			}}
		>
			<header className={styles.portfolioDetail__header}>
				<div>
					<h2 id="portfolio-detail-title">Detalle de la posición</h2>
					<p id="portfolio-detail-description">
						Información financiera y actividad del jugador
					</p>
				</div>
				<button
					ref={closeRef}
					type="button"
					onClick={onClose}
					aria-label="Cerrar detalle de la posición"
				>
					×
				</button>
			</header>
			<div className={styles.portfolioDetail__body}>
				<section
					className={styles.portfolioDetail__identity}
					aria-label="Información del jugador"
				>
					<div className={styles.portfolioDetail__player}>
						<div className={styles.portfolioDetail__portrait}>
							<PortfolioPortrait imageUrl={p.imageUrl} name={p.playerName} />
						</div>
						<div>
							<h3>{p.playerName ?? portfolioLabels.missing}</h3>
							<p>{p.teamName ?? portfolioLabels.missing}</p>
							<div className={styles.portfolioDetail__statusLine}>
								<span>{p.position ?? portfolioLabels.missing}</span>
								<span
									className={`${styles.portfolioDetail__badge} ${p.status === "CLOSED" ? styles.portfolioDetail__badge_closed : ""}`}
								>
									<span aria-hidden="true">●</span>
									{p.status === "OPEN" ? "ABIERTA" : "CERRADA"}
								</span>
							</div>
						</div>
					</div>
					<dl className={styles.portfolioDetail__identifiers}>
						<div>
							<dt>ID del jugador</dt>
							<dd>{p.playerId}</dd>
						</div>
						<div>
							<dt>Jugador activo</dt>
							<dd>{p.active ? "Sí" : "No"}</dd>
						</div>
						<div>
							<dt>Estado de valoración</dt>
							<dd>{p.valuationStatus}</dd>
						</div>
					</dl>
				</section>

				<div className={styles.portfolioDetail__metrics}>
					<section className={styles.portfolioDetail__metric}>
						<div>
							<Coins aria-hidden="true" />
							<h3>Costo de adquisición</h3>
						</div>
						<strong>{formatCredits(p.acquisitionCost, true)}</strong>
					</section>
					<section className={styles.portfolioDetail__metric}>
						<div>
							<TrendingUp aria-hidden="true" />
							<h3>Valor actual</h3>
						</div>
						<strong>{formatCredits(p.currentValue, true)}</strong>
					</section>
					<section
						className={`${styles.portfolioDetail__metric} ${resultClass}`}
					>
						<div>
							<TrendingUp aria-hidden="true" />
							<h3>Resultado total</h3>
						</div>
						<strong>{formatCredits(p.totalProfitLoss, true)}</strong>
					</section>
				</div>

				<div className={styles.portfolioDetail__sections}>
					<DetailPanel
						title="Tenencia"
						icon={Package}
						rows={[
							{ label: "Tokens totales", value: p.quantity },
							{ label: "Disponibles", value: p.availableQuantity },
							{ label: "Reservados", value: p.reservedQuantity },
							{
								label: "Peso del portfolio",
								value: formatPercentage(p.portfolioWeightPercentage),
							},
						]}
					>
						<div
							className={styles.portfolioDetail__track}
							role="img"
							aria-label={`${p.availableQuantity} disponibles y ${p.reservedQuantity} reservados de ${p.quantity} tokens`}
						>
							<span style={{ width: `${available}%` }} />
							<span style={{ width: `${reserved}%` }} />
						</div>
						<div className={styles.portfolioDetail__legend}>
							<span>
								Disponibles {formatPercentage(available)} ({p.availableQuantity}
								)
							</span>
							<span>
								Reservados {formatPercentage(reserved)} ({p.reservedQuantity})
							</span>
						</div>
					</DetailPanel>
					<DetailPanel
						title="Inversión y cotización"
						icon={BarChart3}
						rows={[
							{
								label: "Precio promedio",
								value: formatCredits(p.averageUnitPrice, true),
							},
							{ label: "Precio actual", value: quote },
							{
								label: "Valor actual",
								value: formatCredits(p.currentValue, true),
							},
							{
								label: "Última cotización",
								value: formatDateTime(p.priceUpdatedAt),
							},
							{ label: "Variación", value: portfolioLabels.missing },
						]}
					/>
					<DetailPanel
						title="Resultados financieros"
						icon={BarChart3}
						rows={[
							{
								label: "Resultado realizado",
								value: formatCredits(p.realizedProfitLoss, true),
								result: p.realizedProfitLoss,
							},
							{
								label: "Resultado no realizado",
								value: formatCredits(p.unrealizedProfitLoss, true),
								result: p.unrealizedProfitLoss,
							},
							{
								label: "Resultado total",
								value: formatCredits(p.totalProfitLoss, true),
								result: p.totalProfitLoss,
							},
							{ label: "Rentabilidad total", value: portfolioLabels.missing },
						]}
					/>
					<DetailPanel
						title="Actividad"
						icon={Activity}
						rows={[
							{ label: "Compras realizadas", value: p.totalBuyTransactions },
							{ label: "Ventas realizadas", value: p.totalSellTransactions },
							{ label: "Fecha de apertura", value: formatDateTime(p.openedAt) },
							{
								label: "Última transacción",
								value: formatDateTime(p.lastTransactionAt),
							},
						]}
					/>
				</div>
				<DetailPanel
					title="Información adicional"
					icon={FileText}
					rows={[
						{ label: "Estado de posición", value: p.status },
						{
							label: "Fecha de cierre",
							value: p.closedAt
								? formatDateTime(p.closedAt)
								: p.status === "OPEN"
									? "No aplica"
									: portfolioLabels.missing,
						},
					]}
				/>
				<p className={styles.portfolioDetail__note}>
					Importes en créditos · Fechas locales.
					<br />
					Variación y rentabilidad total no disponibles en los datos de la
					posición.
				</p>
			</div>
		</dialog>
	);
}
