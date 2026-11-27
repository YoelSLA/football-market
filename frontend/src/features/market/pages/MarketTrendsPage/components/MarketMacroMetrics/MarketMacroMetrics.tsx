import type { ReactNode } from "react";
import { TokenSymbol } from "@/shared/components";
import { Activity, Coins, Receipt, ShoppingCart } from "@/shared/icons";
import styles from "./MarketMacroMetrics.module.scss";

export interface MarketMacroMetricsProps {
	totalTradedCredits: number;
	totalTradedTokens: number;
	totalTransactions: number;
}

const VARIANT_CLASSES = {
	blue: styles.item_blue,
	violet: styles.item_violet,
	indigo: styles.item_indigo,
	rose: styles.item_rose,
} as const;

export default function MarketMacroMetrics({
	totalTradedCredits,
	totalTradedTokens,
	totalTransactions,
}: MarketMacroMetricsProps) {
	const averageTicket =
		totalTransactions > 0 ? totalTradedCredits / totalTransactions : 0;
	const averageTokenPrice =
		totalTradedTokens > 0 ? totalTradedCredits / totalTradedTokens : 0;

	const metrics: Array<{
		label: string;
		value: ReactNode;
		unit?: ReactNode;
		detail: string;
		icon: typeof Coins;
		variant: "blue" | "violet" | "indigo" | "rose";
	}> = [
		{
			label: "Volumen Operado",
			value: totalTradedCredits.toLocaleString("es-AR", {
				minimumFractionDigits: 2,
				maximumFractionDigits: 2,
			}),
			unit: "₵",
			detail: "Créditos negociados en 24h",
			icon: Coins,
			variant: "indigo",
		},
		{
			label: "Tokens Transferidos",
			value: totalTradedTokens.toLocaleString("es-AR"),
			unit: <TokenSymbol size="sm" />,
			detail: "Volumen total adquirido",
			icon: ShoppingCart,
			variant: "blue",
		},
		{
			label: "Transacciones",
			value: totalTransactions.toLocaleString("es-AR"),
			unit: "órdenes",
			detail: "Compras y ventas confirmadas",
			icon: Activity,
			variant: "violet",
		},
		{
			label: "Ticket Promedio",
			value: averageTicket.toLocaleString("es-AR", {
				minimumFractionDigits: 2,
				maximumFractionDigits: 2,
			}),
			unit: "₵",
			detail: `~${averageTokenPrice.toLocaleString("es-AR", {
				minimumFractionDigits: 2,
				maximumFractionDigits: 2,
			})} ₵ por token`,
			icon: Receipt,
			variant: "rose",
		},
	];

	return (
		<section aria-label="Indicadores macro del mercado" className={styles.grid}>
			{metrics.map(({ label, value, unit, detail, icon: Icon, variant }) => (
				<div
					key={label}
					className={`${styles.item} ${VARIANT_CLASSES[variant]}`}
				>
					<span className={styles.iconWrapper} aria-hidden="true">
						<Icon className={styles.icon} />
					</span>
					<div className={styles.body}>
						<span className={styles.label}>{label}</span>
						<strong className={styles.value}>
							<span>{value}</span>
							{unit && <span className={styles.unit}>{unit}</span>}
						</strong>
						<span className={styles.detail}>{detail}</span>
					</div>
				</div>
			))}
		</section>
	);
}
