import type { PortfolioValueRowData } from "@/features/portfolio/types";
import styles from "./PortfolioValueRow.module.scss";

interface Props extends PortfolioValueRowData {
	prominent?: boolean;
}

export default function PortfolioValueRow({
	label,
	value,
	result,
	icon: Icon,
	iconTone = "cyan",
	iconTile = false,
	emphasis = false,
	prominent = false,
}: Props) {
	const toneClass =
		result == null || result === 0
			? ""
			: result > 0
				? styles.portfolioValueRow_positive
				: styles.portfolioValueRow_negative;
	const iconClass = {
		cyan: styles.portfolioValueRow__icon_cyan,
		violet: styles.portfolioValueRow__icon_violet,
		green: styles.portfolioValueRow__icon_green,
		gold: styles.portfolioValueRow__icon_gold,
	}[iconTone];

	return (
		<div
			className={[
				styles.portfolioValueRow,
				prominent ? styles.portfolioValueRow_prominent : "",
				emphasis ? styles.portfolioValueRow_emphasis : "",
				toneClass,
			]
				.filter(Boolean)
				.join(" ")}
		>
			<dt className={styles.portfolioValueRow__label}>
				{Icon && (
					<span
						className={[
							styles.portfolioValueRow__iconFrame,
							iconClass,
							iconTile ? styles.portfolioValueRow__iconFrame_tinted : "",
						]
							.filter(Boolean)
							.join(" ")}
					>
						<Icon
							className={styles.portfolioValueRow__icon}
							aria-hidden="true"
						/>
					</span>
				)}
				{label}
			</dt>
			<dd className={styles.portfolioValueRow__value}>{value}</dd>
		</div>
	);
}
