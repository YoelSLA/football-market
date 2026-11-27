import { TokenSymbol } from "@/shared/components";
import { CREDIT_SYMBOL } from "@/shared/constants";
import styles from "./SorareCardFooter.module.scss";

interface Props {
	price: number;
	tokensOwned?: number;
	deltaPercentage?: number | null;
	valueRatio?: number;
}

export default function SorareCardFooter({
	price,
	tokensOwned,
	deltaPercentage,
	valueRatio,
}: Props) {
	const formatPrice = (amount: number) => {
		return `${amount.toLocaleString("es-ES", {
			minimumFractionDigits: 2,
			maximumFractionDigits: 2,
		})} ${CREDIT_SYMBOL}`;
	};

	const formatDelta = (pct: number | null) => {
		if (pct === null) return null;
		const sign = pct > 0 ? "+" : "";
		return `${sign}${pct.toFixed(2)}%`;
	};

	const isPositive = (deltaPercentage ?? 0) >= 0;

	return (
		<footer className={styles.footer}>
			<div className={styles.priceBlock}>
				<span className={styles.priceLabel}>Cotización</span>
				<span className={styles.priceValue}>{formatPrice(price)}</span>
			</div>

			<div className={styles.metaBlock}>
				{tokensOwned !== undefined && (
					<span className={styles.tokensBadge}>
						{tokensOwned} <TokenSymbol size="xs" />
					</span>
				)}

				{deltaPercentage !== undefined && deltaPercentage !== null && (
					<span
						className={`${styles.deltaBadge} ${isPositive ? styles.deltaUp : styles.deltaDown}`}
					>
						{formatDelta(deltaPercentage)}
					</span>
				)}

				{valueRatio !== undefined && (
					<span className={styles.ratioBadge} title="Score por crédito">
						{valueRatio.toFixed(1)} pts/k{CREDIT_SYMBOL}
					</span>
				)}
			</div>
		</footer>
	);
}
