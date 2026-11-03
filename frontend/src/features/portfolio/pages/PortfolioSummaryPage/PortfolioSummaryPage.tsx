import { PageLayout } from "@/app/layouts";
import { usePortfolioSummaryPage } from "@/features/portfolio/hooks";
import type { PortfolioSummary } from "@/features/portfolio/types";
import PortfolioSummaryContent from "./PortfolioSummaryContent";
import PortfolioSummaryHeader from "./PortfolioSummaryHeader";
import styles from "./PortfolioSummaryPage.module.scss";

export default function PortfolioSummaryPage() {
	const page = usePortfolioSummaryPage();
	const s: PortfolioSummary | null | undefined = page.summary;

	return (
		<div className={styles.portfolioSummary} aria-busy={page.isLoading}>
			<PageLayout>
				<div className={styles.portfolioSummary__panel}>
					<PortfolioSummaryHeader
						title="Mi portfolio"
						description="Resumen general de tus inversiones"
					/>
					<PortfolioSummaryContent
						isLoading={page.isLoading}
						errorMessage={page.errorMessage}
						empty={page.empty}
						summary={s}
						onRetry={page.retry}
					/>
				</div>
			</PageLayout>
		</div>
	);
}
