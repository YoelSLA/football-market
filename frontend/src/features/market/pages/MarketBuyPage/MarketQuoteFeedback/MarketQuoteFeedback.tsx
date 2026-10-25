import type { MarketQuoteStatus } from "@/features/market/types";
import { getMarketQuoteMessage } from "@/features/market/utils";
import styles from "./MarketQuoteFeedback.module.scss";

interface Props {
	status: MarketQuoteStatus;
	httpStatus?: number;
	canRetry: boolean;
	onRetry: () => void;
}

export default function MarketQuoteFeedback({ status, httpStatus, canRetry, onRetry }: Props) {
	if (status === "available") return null;
	return (
		<div className={styles.marketQuoteFeedback}>
			<p role="status">{getMarketQuoteMessage(status, httpStatus)}</p>
			{canRetry && <button type="button" onClick={onRetry}>Reintentar cotización</button>}
		</div>
	);
}
