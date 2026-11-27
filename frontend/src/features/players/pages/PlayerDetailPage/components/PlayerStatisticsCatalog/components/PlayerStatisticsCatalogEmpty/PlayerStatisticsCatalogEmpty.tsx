import { Button } from "@/shared/components";
import styles from "./PlayerStatisticsCatalogEmpty.module.scss";

interface Props {
	onClear: () => void;
}

export default function PlayerStatisticsCatalogEmpty({ onClear }: Props) {
	return (
		<div className={styles.playerStatisticsCatalogEmpty} role="status">
			<p>
				No se encontraron estadísticas que coincidan con los filtros aplicados.
			</p>
			<Button type="button" onClick={onClear}>
				Limpiar filtros
			</Button>
		</div>
	);
}
