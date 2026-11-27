import { portfolioLabels } from "@/features/portfolio/constants";
import type { PositionFilter } from "@/features/portfolio/types";
import {
	CheckCircle2,
	Circle,
	LayoutGrid,
	type LucideIcon,
} from "@/shared/icons";
import styles from "./PortfolioStatusFilter.module.scss";

interface Props {
	value: PositionFilter;
	onChange: (value: PositionFilter) => void;
}

const ICONS = {
	ALL: LayoutGrid,
	OPEN: Circle,
	CLOSED: CheckCircle2,
} satisfies Record<PositionFilter, LucideIcon>;

export default function PortfolioStatusFilter({ value, onChange }: Props) {
	return (
		<fieldset
			className={styles.portfolioFilter}
			aria-label="Estado de las posiciones"
		>
			{portfolioLabels.filters.map((filter) => {
				const Icon = ICONS[filter.value];
				const isActive = value === filter.value;

				return (
					<button
						key={filter.value}
						type="button"
						aria-pressed={isActive}
						data-filter={filter.value}
						className={[
							styles.portfolioFilter__option,
							isActive && styles.portfolioFilter__option_active,
						]
							.filter(Boolean)
							.join(" ")}
						onClick={() => onChange(filter.value)}
					>
						<Icon
							size={16}
							aria-hidden="true"
							className={styles.portfolioFilter__icon}
						/>
						{filter.label}
					</button>
				);
			})}
		</fieldset>
	);
}
