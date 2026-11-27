import MarketQuoteFeedback from "@/features/market/components/MarketQuoteFeedback";
import type { MarketQuote, MarketQuoteStatus } from "@/features/market/types";
import {
	formatCentimes,
	formatCredits,
	formatQuoteCalculatedAt,
} from "@/features/market/utils";
import { TokenSymbol } from "@/shared/components";
import styles from "./SellModalHoldingInfo.module.scss";

interface Props {
	quote: MarketQuote | null;
	quoteStatus: MarketQuoteStatus;
	quoteHttpStatus?: number;
	availableQuantity: number;
	reservedQuantity: number;
	canRetryQuote: boolean;
	onRetryQuote: () => void;
}

export default function SellModalHoldingInfo({
	quote,
	quoteStatus,
	quoteHttpStatus,
	availableQuantity,
	reservedQuantity,
	canRetryQuote,
	onRetryQuote,
}: Props) {
	const isQuoteAvailable = quoteStatus === "available" && quote !== null;
	const totalQuantity = availableQuantity + reservedQuantity;

	return (
		<div className={styles.holdingInfo}>
			<div className={styles.holdingInfo__grid}>
				<div className={styles.holdingInfo__tile}>
					<span className={styles.holdingInfo__label}>
						Cotización de referencia
					</span>
					<strong className={styles.holdingInfo__price}>
						{isQuoteAvailable ? formatCredits(quote.price) : "—"}
					</strong>
					<span className={styles.holdingInfo__caption}>
						{isQuoteAvailable
							? `${formatCentimes(quote.price)} · ${formatQuoteCalculatedAt(
									quote.calculatedAt,
								)}`
							: quoteStatus === "loading"
								? "Consultando cotización…"
								: "No disponible"}
					</span>
				</div>

				<div className={styles.holdingInfo__tile}>
					<span className={styles.holdingInfo__label}>Tokens disponibles</span>
					<strong
						className={`${styles.holdingInfo__available} ${
							availableQuantity > 0 ? styles.holdingInfo__available_active : ""
						}`}
					>
						{availableQuantity} <TokenSymbol size="sm" />
					</strong>
					<span className={styles.holdingInfo__caption}>
						{reservedQuantity} en reserva · Total: {totalQuantity}
					</span>
				</div>
			</div>

			<MarketQuoteFeedback
				status={quoteStatus}
				httpStatus={quoteHttpStatus}
				canRetry={canRetryQuote}
				onRetry={onRetryQuote}
			/>
		</div>
	);
}
