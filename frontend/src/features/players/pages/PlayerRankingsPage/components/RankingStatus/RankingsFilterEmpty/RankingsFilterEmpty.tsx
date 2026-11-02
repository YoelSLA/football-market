import { Button } from "@/shared/components";
import { RotateCw } from "@/shared/icons";
import styles from "./RankingsFilterEmpty.module.scss";

interface Props {
	hasActiveFilters: boolean;
	onClearFilters: () => void;
}

export default function RankingsFilterEmpty({
	hasActiveFilters,
	onClearFilters,
}: Props) {
	return (
		<div className={styles.filterEmptyState} role="status">
			<p className={styles.filterEmptyState__text}>
				No se encontraron jugadores que coincidan con los filtros aplicados.
			</p>

			{hasActiveFilters && (
				<Button
					onClick={onClearFilters}
					className={styles.filterEmptyState__button}
				>
					<RotateCw size={14} aria-hidden="true" />
					<span>Restablecer filtros</span>
				</Button>
			)}
		</div>
	);
}
