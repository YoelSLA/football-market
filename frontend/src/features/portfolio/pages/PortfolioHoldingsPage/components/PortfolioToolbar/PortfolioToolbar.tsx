import type { PositionFilter } from "@/features/portfolio/types";
import PortfolioStatusFilter from "../PortfolioHoldingsHeader/PortfolioStatusFilter";
import styles from "./PortfolioToolbar.module.scss";

interface Props {
	filter: PositionFilter;
	onFilterChange: (filter: PositionFilter) => void;
	totalElements: number;
}

export default function PortfolioToolbar({
	filter,
	onFilterChange,
	totalElements,
}: Props) {
	return (
		<div
			className={styles.portfolioToolbar}
			role="toolbar"
			aria-label="Controles de filtrado de posiciones"
		>
			<div className={styles.portfolioToolbar__left}>
				<PortfolioStatusFilter value={filter} onChange={onFilterChange} />
			</div>
			<div className={styles.portfolioToolbar__right}>
				<span className={styles.portfolioToolbar__count}>
					{totalElements} {totalElements === 1 ? "posición" : "posiciones"}
				</span>
			</div>
		</div>
	);
}
