import { Button } from "@/shared/components";
import { Search } from "@/shared/icons";
import styles from "./PlayerStatisticsSearch.module.scss";

interface Props {
	search: string;
	hasActiveFilters: boolean;
	onSearchChange: (text: string) => void;
	onClear: () => void;
}

export default function PlayerStatisticsSearch({
	search,
	hasActiveFilters,
	onSearchChange,
	onClear,
}: Props) {
	return (
		<div className={styles.playerStatisticsSearch}>
			<label className={styles.playerStatisticsSearch__field}>
				<Search
					size={20}
					aria-hidden="true"
					className={styles.playerStatisticsSearch__icon}
				/>

				<input
					type="search"
					aria-label="Buscar estadística por nombre o clave"
					placeholder="Buscar estadísticas..."
					value={search}
					onChange={(event) => onSearchChange(event.target.value)}
				/>
			</label>

			{hasActiveFilters && (
				<Button
					type="button"
					onClick={onClear}
					className={styles.playerStatisticsSearch__clearBtn}
				>
					Limpiar
				</Button>
			)}
		</div>
	);
}
