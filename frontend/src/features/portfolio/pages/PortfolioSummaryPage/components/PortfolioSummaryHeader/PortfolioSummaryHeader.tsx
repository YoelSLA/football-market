import { BarChart3, ChevronRight, Info } from "@/shared/icons";
import styles from "./PortfolioSummaryHeader.module.scss";

interface Props {
	title: string;
	description: string;
}

export default function PortfolioSummaryHeader({ title, description }: Props) {
	return (
		<header className={styles.portfolioSummaryHeader}>
			<span className={styles.portfolioSummaryHeader__icon} aria-hidden="true">
				<BarChart3 />
			</span>
			<div className={styles.portfolioSummaryHeader__heading}>
				<h1 className={styles.portfolioSummaryHeader__title}>{title}</h1>
				<p className={styles.portfolioSummaryHeader__description}>
					{description}
				</p>
			</div>
			<aside
				className={styles.portfolioSummaryHeader__notice}
				aria-label="Información sobre créditos"
			>
				<Info aria-hidden="true" />
				<p>
					Los valores se expresan en créditos.
					<strong>Datos de tu portfolio</strong>
				</p>
				<ChevronRight aria-hidden="true" />
			</aside>
		</header>
	);
}
