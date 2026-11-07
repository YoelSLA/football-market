import { portfolioLabels } from "@/features/portfolio/constants";
import type { PortfolioSummary } from "@/features/portfolio/types";
import { Button } from "@/shared/components";
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
				<Button
					type="button"
					onClick={onRetry}
					className={styles.portfolioSummaryContent__retryButton}
				>
					Reintentar
				</Button>
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
