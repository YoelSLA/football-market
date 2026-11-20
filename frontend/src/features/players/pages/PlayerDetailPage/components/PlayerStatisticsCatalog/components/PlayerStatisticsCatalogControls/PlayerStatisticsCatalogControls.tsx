import styles from "./PlayerStatisticsCatalogControls.module.scss";
import PlayerStatisticsCategoryFilters from "./PlayerStatisticsCategoryFilters";
import PlayerStatisticsRelevanceLegend from "./PlayerStatisticsRelevanceLegend";
import PlayerStatisticsSearch from "./PlayerStatisticsSearch";

interface Props {
	search: string;
	category: string;
	categoryCounts: Record<string, number>;
	hasActiveFilters: boolean;
	onSearchChange: (text: string) => void;
	onCategoryChange: (category: string) => void;
	onClear: () => void;
}

export default function PlayerStatisticsCatalogControls({
	search,
	category,
	categoryCounts,
	hasActiveFilters,
	onSearchChange,
	onCategoryChange,
	onClear,
}: Props) {
	return (
		<div className={styles.playerStatisticsCatalogControls}>
			<PlayerStatisticsSearch
				search={search}
				hasActiveFilters={hasActiveFilters}
				onSearchChange={onSearchChange}
				onClear={onClear}
			/>

			<PlayerStatisticsCategoryFilters
				category={category}
				categoryCounts={categoryCounts}
				onCategoryChange={onCategoryChange}
			/>

			<PlayerStatisticsRelevanceLegend />
		</div>
	);
}
