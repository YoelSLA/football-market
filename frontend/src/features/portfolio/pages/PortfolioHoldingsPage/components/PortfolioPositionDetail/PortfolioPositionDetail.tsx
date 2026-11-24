import { useRef } from "react";
import { portfolioLabels } from "@/features/portfolio/constants";
import type { PortfolioPosition } from "@/features/portfolio/types";
import {
	formatCredits,
	formatDateTime,
	formatPercentage,
} from "@/features/portfolio/utils";
import { Drawer } from "@/shared/components";
import { Activity, BarChart3, FileText, Package } from "@/shared/icons";
import {
	PositionDetailHeader,
	PositionDetailIdentity,
	PositionDetailMetrics,
	PositionTenureTrack,
} from "./components";
import DetailPanel from "./DetailPanel";
import styles from "./PortfolioPositionDetail.module.scss";

interface Props {
	position: PortfolioPosition;
	onClose: () => void;
}

export default function PortfolioPositionDetail({
	position: p,
	onClose,
}: Props) {
	const closeRef = useRef<HTMLButtonElement>(null);

	const quote =
		p.valuationStatus === "UNVALUED"
			? "Sin cotización"
			: formatCredits(p.currentUnitPrice, true);

	return (
		<Drawer
			isOpen
			onClose={onClose}
			title="Detalle de la posición"
			ariaLabel="Detalle de la posición"
			initialFocusRef={closeRef}
			className={styles.portfolioDetail}
		>
			<PositionDetailHeader onClose={onClose} closeRef={closeRef} />

			<div className={styles.portfolioDetail__body}>
				<PositionDetailIdentity position={p} />

				<PositionDetailMetrics
					acquisitionCost={p.acquisitionCost}
					currentValue={p.currentValue}
					totalProfitLoss={p.totalProfitLoss}
				/>

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
						<PositionTenureTrack
							quantity={p.quantity}
							availableQuantity={p.availableQuantity}
							reservedQuantity={p.reservedQuantity}
						/>
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
		</Drawer>
	);
}
