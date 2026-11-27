import type { LucideIcon } from "@/shared/icons";
import styles from "./SummaryPanelHeader.module.scss";

interface Props {
	title: string;
	subtitle: string;
	icon: LucideIcon;
}

export default function SummaryPanelHeader({
	title,
	subtitle,
	icon: Icon,
}: Props) {
	return (
		<header className={styles.summaryPanelHeader}>
			<div className={styles.summaryPanelHeader__titleGroup}>
				<Icon
					size={28}
					aria-hidden="true"
					className={styles.summaryPanelHeader__icon}
				/>

				<h3 className={styles.summaryPanelHeader__title}>{title}</h3>
			</div>

			<small className={styles.summaryPanelHeader__subtitle}>{subtitle}</small>
		</header>
	);
}
