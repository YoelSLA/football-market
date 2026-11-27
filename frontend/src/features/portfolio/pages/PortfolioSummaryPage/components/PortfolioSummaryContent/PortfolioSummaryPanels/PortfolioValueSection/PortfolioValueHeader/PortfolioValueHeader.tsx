import { ChevronRight, type LucideIcon } from "@/shared/icons";
import styles from "./PortfolioValueHeader.module.scss";

interface Props {
	title: string;
	icon: LucideIcon;
	prominent?: boolean;
}

export default function PortfolioValueHeader({
	title,
	icon: Icon,
	prominent = false,
}: Props) {
	return (
		<header
			className={`${styles.portfolioValueHeader} ${prominent ? styles.portfolioValueHeader_prominent : ""}`.trim()}
		>
			<span className={styles.portfolioValueHeader__tile}>
				<Icon
					className={styles.portfolioValueHeader__icon}
					aria-hidden="true"
				/>
			</span>
			<h2 className={styles.portfolioValueHeader__title}>{title}</h2>
			{prominent && (
				<ChevronRight
					className={styles.portfolioValueHeader__chevron}
					aria-hidden="true"
				/>
			)}
		</header>
	);
}
