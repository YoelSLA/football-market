import type { ResolvedSummaryMetric } from "@/features/players/utils";
import type { LucideIcon } from "@/shared/icons";
import SummaryMetricRow from "./SummaryMetricRow";
import styles from "./SummaryPanel.module.scss";
import SummaryPanelHeader from "./SummaryPanelHeader";

interface Props {
	title: string;
	subtitle: string;
	icon: LucideIcon;
	metrics: ResolvedSummaryMetric[];
}

export default function SummaryPanel({
	title,
	subtitle,
	icon,
	metrics,
}: Props) {
	return (
		<section className={styles.summaryPanel} aria-label={title}>
			<SummaryPanelHeader title={title} subtitle={subtitle} icon={icon} />

			<ul className={styles.summaryPanel__list}>
				{metrics.map((metric) => (
					<SummaryMetricRow key={metric.key} metric={metric} />
				))}
			</ul>
		</section>
	);
}
