import clsx from "clsx";
import type { CatalogCategoryGroup } from "@/features/players/utils";
import { formatStatisticsValue, getMetricIcon } from "@/features/players/utils";
import styles from "./PlayerStatisticsCatalogCard.module.scss";

type CatalogMetric = CatalogCategoryGroup["metrics"][number];

interface Props {
	item: CatalogMetric;
}

export default function PlayerStatisticsCatalogCard({ item }: Props) {
	const Icon = item.icon ?? getMetricIcon(item.key);
	const isAbsent = item.value === null;

	const priorityClass =
		item.priority === 0
			? styles.playerStatisticsCatalogCard__priority_principal
			: item.priority === 1
				? styles.playerStatisticsCatalogCard__priority_secondary
				: styles.playerStatisticsCatalogCard__priority_low;

	return (
		<article
			className={clsx(
				styles.playerStatisticsCatalogCard,
				isAbsent && styles.playerStatisticsCatalogCard_absent,
			)}
		>
			<span
				className={clsx(
					styles.playerStatisticsCatalogCard__priority,
					priorityClass,
				)}
				aria-hidden="true"
			/>

			<span className={styles.srOnly}>Relevancia: {item.priorityLabel}</span>

			<div className={styles.playerStatisticsCatalogCard__iconContainer}>
				<Icon
					size={28}
					strokeWidth={2}
					className={styles.playerStatisticsCatalogCard__icon}
					aria-hidden="true"
				/>
			</div>

			<div className={styles.playerStatisticsCatalogCard__content}>
				<h4
					className={styles.playerStatisticsCatalogCard__title}
					title={item.label}
				>
					{item.label}
				</h4>

				<span
					className={clsx(
						styles.playerStatisticsCatalogCard__value,
						isAbsent && styles.playerStatisticsCatalogCard__value_absent,
					)}
				>
					{formatStatisticsValue(item.value)}
					{item.unit === "%" && !isAbsent ? " %" : ""}
				</span>

				{item.unit && item.unit !== "%" && (
					<span
						className={styles.playerStatisticsCatalogCard__unit}
						title={item.unit}
					>
						{item.unit}
					</span>
				)}
			</div>
		</article>
	);
}
