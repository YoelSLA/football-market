import type { ReactNode } from "react";
import type { PortfolioValueRowData } from "@/features/portfolio/types";
import type { LucideIcon } from "@/shared/icons";
import PortfolioValueHeader from "./PortfolioValueHeader";
import PortfolioValueRows from "./PortfolioValueRows";
import styles from "./PortfolioValueSection.module.scss";

interface Props {
	title: string;
	icon: LucideIcon;
	rows?: PortfolioValueRowData[];
	aside?: ReactNode;
	body?: ReactNode;
	footer?: ReactNode;
	appearance?: "default" | "dashboard";
	action?: boolean;
	dividedAside?: boolean;
}

export default function PortfolioValueSection({
	title,
	icon,
	rows,
	aside,
	body,
	footer,
	appearance = "default",
	action = false,
	dividedAside = false,
}: Props) {
	const dashboard = appearance === "dashboard";
	const className = [
		styles.portfolioValueSection,
		dashboard ? styles.portfolioValueSection_dashboard : "",
		dashboard && action ? styles.portfolioValueSection_action : "",
	]
		.filter(Boolean)
		.join(" ");
	return (
		<section className={className}>
			<PortfolioValueHeader title={title} icon={icon} prominent={dashboard} />

			<div
				className={`${styles.portfolioValueSection__body} ${
					aside ? styles.portfolioValueSection__body_withAside : ""
				}`.trim()}
			>
				{rows && <PortfolioValueRows rows={rows} prominent={dashboard} />}

				{body && (
					<p className={styles.portfolioValueSection__bodyText}>{body}</p>
				)}

				{aside && (
					<div
						className={`${styles.portfolioValueSection__aside} ${
							dashboard && dividedAside
								? styles.portfolioValueSection__aside_divided
								: ""
						}`.trim()}
					>
						{aside}
					</div>
				)}
			</div>

			{footer && (
				<div className={styles.portfolioValueSection__footer}>{footer}</div>
			)}
		</section>
	);
}
