import type { PortfolioValueRowData } from "@/features/portfolio/types";
import PortfolioValueRow from "./PortfolioValueRow";
import styles from "./PortfolioValueRows.module.scss";

interface Props {
	rows: PortfolioValueRowData[];
	prominent?: boolean;
}

export default function PortfolioValueRows({ rows, prominent = false }: Props) {
	return (
		<dl className={styles.portfolioValueRows}>
			{rows.map((row) => (
				<PortfolioValueRow key={row.label} {...row} prominent={prominent} />
			))}
		</dl>
	);
}
