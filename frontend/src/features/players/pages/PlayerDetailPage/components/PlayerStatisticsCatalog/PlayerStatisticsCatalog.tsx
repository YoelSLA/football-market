import type { PlayerMetric } from "@/features/players/types";
import {
	getCategoryCounts,
	getPlayerStatisticsCatalog,
	getPlayerStatisticsCatalogGroups,
} from "@/features/players/utils";
import PlayerStatisticsCatalogControls from "./components/PlayerStatisticsCatalogControls";
import PlayerStatisticsCatalogEmpty from "./components/PlayerStatisticsCatalogEmpty";
import PlayerStatisticsCatalogGroup from "./components/PlayerStatisticsCatalogGroup";
import styles from "./PlayerStatisticsCatalog.module.scss";

interface Props {
	metrics: PlayerMetric[];
	position: string;
	search: string;
	category: string;
	onSearchChange: (text: string) => void;
	onCategoryChange: (category: string) => void;
	onClear: () => void;
}

export default function PlayerStatisticsCatalog({
	metrics,
	position,
	search,
	category,
	onSearchChange,
	onCategoryChange,
	onClear,
}: Props) {
	const categoryCounts = getCategoryCounts();
	const catalogMetrics = getPlayerStatisticsCatalog(
		metrics,
		position,
		search,
		category,
	);
	const groups = getPlayerStatisticsCatalogGroups(catalogMetrics);
	const hasActiveFilters = Boolean(search || category !== "all");

	return (
		<section
			className={styles.playerStatisticsCatalog}
			aria-label="Catálogo completo de estadísticas"
		>
			<div className={styles.playerStatisticsCatalog__controlsWrapper}>
				<PlayerStatisticsCatalogControls
					search={search}
					category={category}
					categoryCounts={categoryCounts}
					hasActiveFilters={hasActiveFilters}
					onSearchChange={onSearchChange}
					onCategoryChange={onCategoryChange}
					onClear={onClear}
				/>
			</div>

			{groups.length === 0 ? (
				<PlayerStatisticsCatalogEmpty onClear={onClear} />
			) : (
				<div className={styles.playerStatisticsCatalog__results}>
					{groups.map((group) => (
						<PlayerStatisticsCatalogGroup key={group.category} group={group} />
					))}
				</div>
			)}
		</section>
	);
}
