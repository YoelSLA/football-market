import { Package } from "@/shared/icons";
import styles from "./PortfolioHoldingsHeader.module.scss";
import PortfolioStatusFilter from "./PortfolioStatusFilter";

interface Props {
	filter: "ALL" | "OPEN" | "CLOSED";
	onFilterChange: (filter: "ALL" | "OPEN" | "CLOSED") => void;
	totalElements: number;
}

export default function PortfolioHoldingsHeader({
	filter,
	onFilterChange,
	totalElements,
}: Props) {
	return (
		<header className={styles.portfolioHoldingsHeader}>
			<div className={styles.portfolioHoldingsHeader__left}>
				<div className={styles.portfolioHoldingsHeader__iconWrapper}>
					<Package className={styles.portfolioHoldingsHeader__icon} />
				</div>
				<div className={styles.portfolioHoldingsHeader__titleWrapper}>
					<h1 className={styles.portfolioHoldingsHeader__title}>
						Mis tenencias
					</h1>
					<p className={styles.portfolioHoldingsHeader__description}>
						Consultá tus posiciones por jugador y sus resultados financieros.
					</p>
				</div>
			</div>
			<div className={styles.portfolioHoldingsHeader__right}>
				<PortfolioStatusFilter value={filter} onChange={onFilterChange} />
				<span className={styles.portfolioHoldingsHeader__count}>
					{totalElements} posiciones
				</span>
			</div>
		</header>
	);
}
