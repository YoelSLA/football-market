import type { PlayerQuote } from "@/features/players/types";
import { formatDate } from "@/features/players/utils";
import { TokenSymbol } from "@/shared/components";
import { CREDIT_SYMBOL } from "@/shared/constants";
import styles from "./QuoteHistoryTable.module.scss";

interface Props {
	quote: PlayerQuote;
}

export function QuoteHistoryTableRow({ quote }: Props) {
	const pct = quote.percentageChange ?? 0;
	const isUp = quote.direction === "up" || pct > 0;
	const isDown = quote.direction === "down" || pct < 0;

	const netChange = quote.absoluteChange;
	const isNetUp = netChange !== null && netChange > 0;
	const isNetDown = netChange !== null && netChange < 0;

	const netDemand = quote.breakdown?.netDemandTokens ?? 0;

	return (
		<tr>
			<td className={styles.td_date}>
				<span className={styles.dateValue}>
					{formatDate(quote.effectiveDate)}
				</span>
			</td>

			<td className={styles.td_prevPrice}>
				<span className={styles.prevPriceValue}>
					{quote.previousPrice !== null && quote.previousPrice !== undefined
						? `${quote.previousPrice.toFixed(2).replace(".", ",")} ${CREDIT_SYMBOL}`
						: "—"}
				</span>
			</td>

			<td className={styles.td_price}>
				<div className={styles.priceContainer}>
					<strong className={styles.priceValue}>
						{quote.price.toFixed(2).replace(".", ",")}
					</strong>
					<span className={styles.priceUnit}>{CREDIT_SYMBOL}</span>
				</div>
			</td>

			<td className={styles.td_netChange}>
				<span
					className={`${styles.netChangeValue} ${
						isNetUp
							? styles.netChangeValue_up
							: isNetDown
								? styles.netChangeValue_down
								: styles.netChangeValue_flat
					}`}
				>
					{netChange !== null && netChange !== undefined
						? `${netChange > 0 ? "+" : ""}${netChange.toFixed(2).replace(".", ",")} ${CREDIT_SYMBOL}`
						: "—"}
				</span>
			</td>

			<td className={styles.td_variation}>
				<span
					className={`${styles.variationBadge} ${
						isUp
							? styles.variationBadge_up
							: isDown
								? styles.variationBadge_down
								: styles.variationBadge_flat
					}`}
				>
					{isUp
						? `▲ +${pct.toFixed(2).replace(".", ",")} %`
						: isDown
							? `▼ ${pct.toFixed(2).replace(".", ",")} %`
							: "● 0,00 %"}
				</span>
			</td>

			<td className={styles.td_demand}>
				<span
					className={`${styles.demandValue} ${
						netDemand > 0
							? styles.demandValue_positive
							: netDemand < 0
								? styles.demandValue_negative
								: ""
					}`}
				>
					{netDemand > 0 ? "+" : ""}
					{netDemand} <TokenSymbol size="xs" />
				</span>
			</td>
		</tr>
	);
}
