import type { TransactionHistorySummary } from "@/features/portfolio/types";
import { formatCount, formatCredits } from "@/features/portfolio/utils";
import { Coins, ShoppingCart, Tag, Wallet } from "@/shared/icons";
import styles from "./TransactionHistoryMetrics.module.scss";

interface Props {
	summary: TransactionHistorySummary;
}

export default function TransactionHistoryMetrics({ summary }: Props) {
	const metrics = [
		{
			label: "Compras",
			value: formatCount(summary.buyTransactions, "operación", "operaciones"),
			detail: formatCount(summary.boughtTokens, "token", "tokens"),
			icon: ShoppingCart,
		},
		{
			label: "Ventas",
			value: formatCount(summary.sellTransactions, "operación", "operaciones"),
			detail: formatCount(summary.soldTokens, "token", "tokens"),
			icon: Tag,
		},
		{
			label: "Volumen comprado",
			value: formatCredits(summary.purchasedVolume),
			detail: "Importe histórico",
			icon: Coins,
		},
		{
			label: "Volumen vendido",
			value: formatCredits(summary.soldVolume),
			detail: "Importe histórico",
			icon: Wallet,
		},
	];

	return (
		<section
			aria-label="Indicadores del historial"
			className={styles.transactionHistoryMetrics}
		>
			{metrics.map(({ label, value, detail, icon: Icon }) => (
				<article className={styles.transactionHistoryMetrics__item} key={label}>
					<Icon
						aria-hidden="true"
						className={styles.transactionHistoryMetrics__icon}
					/>
					<div>
						<p className={styles.transactionHistoryMetrics__label}>{label}</p>
						<strong className={styles.transactionHistoryMetrics__value}>
							{value}
						</strong>
						<span className={styles.transactionHistoryMetrics__detail}>
							{detail}
						</span>
					</div>
				</article>
			))}
		</section>
	);
}
