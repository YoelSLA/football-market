import { CATEGORY_ICONS } from "@/features/players/constants";
import type { CatalogCategoryGroup } from "@/features/players/utils";
import PlayerStatisticsCatalogCard from "../PlayerStatisticsCatalogCard";
import styles from "./PlayerStatisticsCatalogGroup.module.scss";

interface Props {
	group: CatalogCategoryGroup;
}

export default function PlayerStatisticsCatalogGroup({ group }: Props) {
	const Icon = CATEGORY_ICONS[group.category];

	return (
		<section
			className={styles.playerStatisticsCatalogGroup}
			aria-labelledby={`cat-${group.category}`}
		>
			<header className={styles.playerStatisticsCatalogGroup__header}>
				<div className={styles.playerStatisticsCatalogGroup__titleWrapper}>
					{Icon && (
						<Icon
							size={24}
							aria-hidden="true"
							className={styles.playerStatisticsCatalogGroup__icon}
						/>
					)}

					<h3
						className={styles.playerStatisticsCatalogGroup__title}
						id={`cat-${group.category}`}
					>
						{group.category}
					</h3>
				</div>

				<span className={styles.playerStatisticsCatalogGroup__count}>
					<span className={styles.srOnly}>
						{`${group.metrics.length} métricas en ${group.category}`}
					</span>
					<span aria-hidden="true">{group.metrics.length}</span>
				</span>
			</header>

			<div className={styles.playerStatisticsCatalogGroup__grid}>
				{group.metrics.map((item) => (
					<PlayerStatisticsCatalogCard key={item.key} item={item} />
				))}
			</div>
		</section>
	);
}
