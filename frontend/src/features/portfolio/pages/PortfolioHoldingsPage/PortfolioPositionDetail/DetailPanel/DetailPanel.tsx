import type { ReactNode } from "react";
import styles from "@/features/portfolio/pages/PortfolioHoldingsPage/PortfolioPositionDetail/DetailPanel/DetailPanel.module.scss";
import type { LucideIcon } from "@/shared/icons";

interface Props {
	title: string;
	icon: LucideIcon;
	rows: { label: string; value: ReactNode; result?: number | null }[];
	children?: ReactNode;
}

export default function DetailPanel({
	title,
	icon: Icon,
	rows,
	children,
}: Props) {
	return (
		<section className={styles.panel}>
			<h3 className={styles.panel__title}>
				<Icon aria-hidden="true" />
				{title}
			</h3>
			<dl className={styles.panel__rows}>
				{rows.map(({ label, value, result }) => (
					<div className={styles.panel__row} key={label}>
						<dt>{label}</dt>
						<dd
							className={
								result != null && result !== 0
									? result > 0
										? styles.panel__positive
										: styles.panel__negative
									: undefined
							}
						>
							{value}
						</dd>
					</div>
				))}
			</dl>
			{children}
		</section>
	);
}
