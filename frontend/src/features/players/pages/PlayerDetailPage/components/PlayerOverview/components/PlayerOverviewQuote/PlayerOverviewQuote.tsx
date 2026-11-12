import type { PlayerQuote } from "@/features/players/types";
import {
	formatDate,
	formatQuoteAbsoluteChange,
	formatQuotePercentageChange,
	formatQuotePrice,
} from "@/features/players/utils";
import { Coins } from "@/shared/icons";
import styles from "./PlayerOverviewQuote.module.scss";

interface Props {
	quote?: PlayerQuote | null;
	className?: string;
}

export default function PlayerOverviewQuote({ quote, className }: Props) {
	const isInitial =
		!quote || quote.previousPrice === null || quote.origin === "INITIAL";
	const isFlat =
		!isInitial &&
		quote != null &&
		(quote.direction === "flat" || quote.absoluteChange === 0);
	const isUp =
		!isInitial &&
		quote != null &&
		quote.direction === "up" &&
		(quote.absoluteChange ?? 0) > 0;
	const isDown =
		!isInitial &&
		quote != null &&
		quote.direction === "down" &&
		(quote.absoluteChange ?? 0) < 0;
	const absoluteChangeFormatted = quote
		? formatQuoteAbsoluteChange(quote.absoluteChange)
		: null;

	return (
		<div className={`${styles.quoteContext} ${className ?? ""}`.trim()}>
			{quote ? (
				<>
					<div className={styles.quoteContext__main}>
						<Coins
							size={22}
							className={styles.quoteContext__coinIcon}
							aria-hidden="true"
						/>
						<strong className={styles.quoteContext__price}>
							{formatQuotePrice(quote.price)}
						</strong>
						<span
							role="status"
							className={`${styles.quoteContext__badge} ${
								isUp
									? styles["quoteContext__badge--up"]
									: isDown
										? styles["quoteContext__badge--down"]
										: isFlat
											? styles["quoteContext__badge--flat"]
											: styles["quoteContext__badge--initial"]
							}`}
							aria-label={
								isUp || isDown
									? `Variación: ${formatQuotePercentageChange(quote.percentageChange)}`
									: isFlat
										? "Sin variación"
										: "Cotización inicial"
							}
						>
							{isUp && (
								<>▲ {formatQuotePercentageChange(quote.percentageChange)}</>
							)}
							{isDown && (
								<>▼ {formatQuotePercentageChange(quote.percentageChange)}</>
							)}
							{isFlat && <>Sin variación</>}
							{isInitial && <>Cotización inicial</>}
						</span>
					</div>
					<p className={styles.quoteContext__meta}>
						<span>Cotización oficial</span>
						<span className={styles.quoteContext__separator} aria-hidden="true">
							·
						</span>
						<span>{formatDate(quote.effectiveDate)}</span>
						{(isUp || isDown) && absoluteChangeFormatted && (
							<>
								<span
									className={styles.quoteContext__separator}
									aria-hidden="true"
								>
									·
								</span>
								<span
									className={
										isUp
											? styles.quoteContext__diff_up
											: styles.quoteContext__diff_down
									}
								>
									{absoluteChangeFormatted}
								</span>
							</>
						)}
					</p>
				</>
			) : (
				<>
					<div className={styles.quoteContext__main}>
						<Coins
							size={22}
							className={styles.quoteContext__coinIcon}
							aria-hidden="true"
						/>
						<strong className={styles.quoteContext__price}>
							Sin cotización
						</strong>
					</div>
					<p className={styles.quoteContext__meta}>
						<span>Pendiente de valuación</span>
					</p>
				</>
			)}
		</div>
	);
}
