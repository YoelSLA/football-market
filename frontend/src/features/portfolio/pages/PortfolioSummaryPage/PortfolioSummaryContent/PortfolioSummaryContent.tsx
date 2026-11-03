import { portfolioLabels } from "@/features/portfolio/constants";
import type { PortfolioSummary } from "@/features/portfolio/types";
import styles from "./PortfolioSummaryContent.module.scss";
import PortfolioSummaryMetrics from "./PortfolioSummaryMetrics";
import PortfolioSummaryPanels from "./PortfolioSummaryPanels";

interface Props {
	isLoading: boolean;
	errorMessage: string | null;
	empty: boolean;
	summary: PortfolioSummary | null | undefined;
	onRetry: () => void;
}

export default function PortfolioSummaryContent({
	isLoading,
	errorMessage,
	empty,
	summary,
	onRetry,
}: Props) {
	if (errorMessage) {
		return (
			<div role="alert" className={styles.portfolioSummaryContent__error}>
				<p>{errorMessage}</p>
				<button
					type="button"
					onClick={onRetry}
					className={styles.portfolioSummaryContent__retryButton}
				>
					Reintentar
				</button>
			</div>
		);
	}

	if (isLoading) {
		return (
			<p role="status" className={styles.portfolioSummaryContent__loading}>
				{portfolioLabels.loading}
			</p>
		);
	}

	if (!summary) {
		return null;
	}

	return (
		<div className={styles.portfolioSummaryContent}>
			{empty && (
				<p role="status" className={styles.portfolioSummaryContent__empty}>
					{portfolioLabels.empty}
				</p>
			)}
			<PortfolioSummaryMetrics summary={summary} />
			<PortfolioSummaryPanels summary={summary} />
		</div>
	);
}
