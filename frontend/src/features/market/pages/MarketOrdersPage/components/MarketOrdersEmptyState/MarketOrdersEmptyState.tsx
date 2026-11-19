import { Link } from "react-router-dom";
import { Puzzle } from "@/shared/icons";
import styles from "./MarketOrdersEmptyState.module.scss";

interface Props {
	isFiltering: boolean;
	onClearFilters: () => void;
}

export default function MarketOrdersEmptyState({
	isFiltering,
	onClearFilters,
}: Props) {
	if (isFiltering) {
		return (
			<section
				className={styles.ordersEmptyState}
				aria-label="Sin resultados de búsqueda"
			>
				<div className={styles.ordersEmptyState__icon} aria-hidden="true">
					<Puzzle size={40} />
				</div>
				<h2 className={styles.ordersEmptyState__title}>
					No se encontraron órdenes
				</h2>
				<p className={styles.ordersEmptyState__description}>
					No hay publicaciones que coincidan con los filtros o el término de
					búsqueda ingresado.
				</p>
				<button
					type="button"
					onClick={onClearFilters}
					className={styles.ordersEmptyState__actionBtn}
				>
					Limpiar filtros
				</button>
			</section>
		);
	}

	return (
		<section
			className={styles.ordersEmptyState}
			aria-label="No hay órdenes registradas"
		>
			<div className={styles.ordersEmptyState__icon} aria-hidden="true">
				<Puzzle size={40} />
			</div>
			<h2 className={styles.ordersEmptyState__title}>
				Aún no tenés órdenes de venta publicadas
			</h2>
			<p className={styles.ordersEmptyState__description}>
				Ofrecé tokens de tu tenencia a otros usuarios en el mercado secundario
				fijando el precio al momento de publicar.
			</p>
			<Link to="/market/sell" className={styles.ordersEmptyState__actionBtn}>
				Vender tokens
			</Link>
		</section>
	);
}
