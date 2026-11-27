import type { ReactNode } from "react";
import type { TransactionHistorySummary } from "@/features/portfolio/types";
import { formatCount, formatCredits } from "@/features/portfolio/utils";
import { TokenSymbol } from "@/shared/components";
import { Coins, ShoppingCart, Tag, Wallet } from "@/shared/icons";
import styles from "./TransactionHistoryMetrics.module.scss";

interface Props {
	summary: TransactionHistorySummary;
	isLoading?: boolean;
}

const VARIANT_CLASSES = {
	blue: styles.transactionHistoryMetrics__item_blue,
	violet: styles.transactionHistoryMetrics__item_violet,
	indigo: styles.transactionHistoryMetrics__item_indigo,
	rose: styles.transactionHistoryMetrics__item_rose,
} as const;

export default function TransactionHistoryMetrics({
	summary,
	isLoading,
}: Props) {
	const metrics: Array<{
		label: string;
		value: ReactNode;
		unit?: ReactNode;
		detail: string;
		tooltip: string;
		icon: typeof ShoppingCart;
		variant: "blue" | "violet" | "indigo" | "rose";
	}> = [
		{
			label: "Tokens comprados",
			value: isLoading
				? "—"
				: `+${summary.boughtTokens.toLocaleString("es-AR")}`,
			unit: <TokenSymbol size="sm" />,
			detail: isLoading
				? "—"
				: `en ${formatCount(summary.buyTransactions, "operación", "operaciones")}`,
			tooltip: `${summary.boughtTokens} tokens adquiridos en ${summary.buyTransactions} compras confirmadas`,
			icon: ShoppingCart,
			variant: "blue",
		},
		{
			label: "Tokens vendidos",
			value: isLoading ? "—" : `-${summary.soldTokens.toLocaleString("es-AR")}`,
			unit: <TokenSymbol size="sm" />,
			detail: isLoading
				? "—"
				: `en ${formatCount(summary.sellTransactions, "operación", "operaciones")}`,
			tooltip: `${summary.soldTokens} tokens liquidados en ${summary.sellTransactions} ventas confirmadas`,
			icon: Tag,
			variant: "violet",
		},
		{
			label: "Volumen comprado",
			value: isLoading ? "—" : formatCredits(summary.purchasedVolume),
			unit: undefined,
			detail: "Inversión acumulada",
			tooltip: `Total invertido en compras: ${formatCredits(summary.purchasedVolume)}`,
			icon: Coins,
			variant: "indigo" as const,
		},
		{
			label: "Volumen vendido",
			value: isLoading ? "—" : formatCredits(summary.soldVolume),
			unit: undefined,
			detail: "Recaudación total",
			tooltip: `Total recaudado en ventas: ${formatCredits(summary.soldVolume)}`,
			icon: Wallet,
			variant: "rose" as const,
		},
	];

	return (
		<section
			aria-label="Indicadores del historial"
			className={styles.transactionHistoryMetrics}
		>
			{metrics.map(
				({ label, value, unit, detail, tooltip, icon: Icon, variant }) => (
					<div
						className={`${styles.transactionHistoryMetrics__item} ${VARIANT_CLASSES[variant]}`}
						key={label}
						title={tooltip}
					>
						<span
							className={styles.transactionHistoryMetrics__iconWrapper}
							aria-hidden="true"
						>
							<Icon className={styles.transactionHistoryMetrics__icon} />
						</span>
						<div className={styles.transactionHistoryMetrics__body}>
							<span className={styles.transactionHistoryMetrics__label}>
								{label}
							</span>
							<strong className={styles.transactionHistoryMetrics__value}>
								<span>{value}</span>
								{unit && (
									<span className={styles.transactionHistoryMetrics__unit}>
										{unit}
									</span>
								)}
							</strong>
							<span className={styles.transactionHistoryMetrics__detail}>
								{detail}
							</span>
						</div>
					</div>
				),
			)}
		</section>
	);
}
