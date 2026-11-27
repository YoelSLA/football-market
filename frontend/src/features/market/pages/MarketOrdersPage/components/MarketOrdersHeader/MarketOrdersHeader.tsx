import type { ReactNode } from "react";
import type { UserOrdersSummary } from "@/features/market/types";
import { formatCreditsAmount } from "@/features/market/utils";
import { TokenSymbol } from "@/shared/components";
import { CheckCircle2, CircleX, Coins, Puzzle, Tag } from "@/shared/icons";
import styles from "./MarketOrdersHeader.module.scss";

interface Props {
	summary: UserOrdersSummary;
	isLoading?: boolean;
}

const VARIANT_CLASSES = {
	blue: styles.ordersHeader__metric_blue,
	violet: styles.ordersHeader__metric_violet,
	indigo: styles.ordersHeader__metric_indigo,
	rose: styles.ordersHeader__metric_rose,
} as const;

export default function MarketOrdersHeader({ summary, isLoading }: Props) {
	const closedCount = summary.cancelledOrders + summary.expiredOrders;

	const metrics: Array<{
		label: string;
		value: ReactNode;
		detail: ReactNode;
		tooltip: string;
		icon: typeof Tag;
		variant: "blue" | "violet" | "indigo" | "rose";
	}> = [
		{
			label: "Órdenes activas",
			value: isLoading
				? "—"
				: `${summary.activeOrders} ${
						summary.activeOrders === 1 ? "activa" : "activas"
					}`,
			detail: isLoading ? (
				"—"
			) : (
				<>
					{summary.activeTokens} <TokenSymbol size="sm" /> en venta
				</>
			),
			tooltip: `${summary.activeOrders} órdenes activas · ${summary.activeTokens} tokens en venta`,
			icon: Tag,
			variant: "blue",
		},
		{
			label: "Valor en venta",
			value: isLoading ? "—" : formatCreditsAmount(summary.activeValue),
			detail: "Importe total en oferta",
			tooltip: `Valor total en oferta: ${formatCreditsAmount(summary.activeValue)}`,
			icon: Coins,
			variant: "violet",
		},
		{
			label: "Órdenes vendidas",
			value: isLoading
				? "—"
				: `${summary.soldOrders} ${
						summary.soldOrders === 1 ? "vendida" : "vendidas"
					}`,
			detail: isLoading ? (
				"—"
			) : (
				<>
					{summary.soldTokens} <TokenSymbol size="sm" /> liquidados
				</>
			),
			tooltip: `${summary.soldOrders} órdenes vendidas · ${summary.soldTokens} tokens liquidados`,
			icon: CheckCircle2,
			variant: "indigo",
		},
		{
			label: "Cerradas / Expiradas",
			value: isLoading
				? "—"
				: `${closedCount} ${closedCount === 1 ? "cerrada" : "cerradas"}`,
			detail: `${summary.cancelledOrders} canceladas · ${summary.expiredOrders} expiradas`,
			tooltip: `${summary.cancelledOrders} canceladas · ${summary.expiredOrders} expiradas`,
			icon: CircleX,
			variant: "rose",
		},
	];

	return (
		<header className={styles.ordersHeader}>
			<div className={styles.ordersHeader__left}>
				<div className={styles.ordersHeader__iconWrapper}>
					<Puzzle aria-hidden="true" className={styles.ordersHeader__icon} />
				</div>
				<div className={styles.ordersHeader__titleWrapper}>
					<h1 className={styles.ordersHeader__title}>Mis órdenes</h1>
					<p className={styles.ordersHeader__description}>
						Gestioná tus publicaciones en el mercado secundario y cancelá
						ofertas activas.
					</p>
				</div>
			</div>

			<section
				aria-label="Indicadores de órdenes"
				className={styles.ordersHeader__metrics}
			>
				{metrics.map(
					({ label, value, detail, tooltip, icon: Icon, variant }) => (
						<div
							key={label}
							className={`${styles.ordersHeader__metricItem} ${VARIANT_CLASSES[variant]}`}
							title={tooltip}
						>
							<span
								className={styles.ordersHeader__metricIconWrapper}
								aria-hidden="true"
							>
								<Icon className={styles.ordersHeader__metricIcon} />
							</span>
							<div className={styles.ordersHeader__metricBody}>
								<span className={styles.ordersHeader__metricLabel}>
									{label}
								</span>
								<strong className={styles.ordersHeader__metricValue}>
									{value}
								</strong>
								<span className={styles.ordersHeader__metricDetail}>
									{detail}
								</span>
							</div>
						</div>
					),
				)}
			</section>
		</header>
	);
}
