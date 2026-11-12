import { CATEGORIES, CATEGORY_ICONS } from "@/features/players/constants";
import styles from "./PlayerStatisticsCategoryFilters.module.scss";

interface Props {
	category: string;
	categoryCounts: Record<string, number>;
	onCategoryChange: (category: string) => void;
}

export default function PlayerStatisticsCategoryFilters({
	category,
	categoryCounts,
	onCategoryChange,
}: Props) {
	const AllIcon = CATEGORY_ICONS.all;

	return (
		<div
			className={styles.playerStatisticsCategoryFilters}
			role="toolbar"
			aria-label="Filtro por categoría"
		>
			<button
				type="button"
				aria-pressed={category === "all"}
				className={styles.playerStatisticsCategoryFilters__button}
				onClick={() => onCategoryChange("all")}
			>
				{AllIcon && <AllIcon size={18} aria-hidden="true" />}
				<span>Todas</span>
				<small>{categoryCounts.all ?? 60}</small>
			</button>

			{CATEGORIES.map((cat) => {
				const Icon = CATEGORY_ICONS[cat];
				const isPressed = category === cat;

				return (
					<button
						key={cat}
						type="button"
						aria-pressed={isPressed}
						className={styles.playerStatisticsCategoryFilters__button}
						onClick={() => onCategoryChange(isPressed ? "all" : cat)}
					>
						{Icon && <Icon size={18} aria-hidden="true" />}
						<span>{cat}</span>
						<small>{categoryCounts[cat] ?? 0}</small>
					</button>
				);
			})}
		</div>
	);
}
