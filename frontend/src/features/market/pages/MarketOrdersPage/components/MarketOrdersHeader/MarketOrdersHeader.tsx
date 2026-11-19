import { Puzzle } from "@/shared/icons";
import styles from "./MarketOrdersHeader.module.scss";

interface Props {
	totalOrders: number;
}

export default function MarketOrdersHeader({ totalOrders }: Props) {
	return (
		<header className={styles.ordersHeader}>
			<div className={styles.ordersHeader__main}>
				<div className={styles.ordersHeader__iconContainer} aria-hidden="true">
					<Puzzle size={28} />
				</div>
				<div className={styles.ordersHeader__text}>
					<h1 className={styles.ordersHeader__title}>MIS ÓRDENES DE VENTA</h1>
					<p className={styles.ordersHeader__subtitle}>
						Gestioná tus publicaciones en el mercado secundario y cancelá
						ofertas activas.
					</p>
				</div>
			</div>
			<div className={styles.ordersHeader__badge}>
				{totalOrders}{" "}
				{totalOrders === 1 ? "orden registrada" : "órdenes registradas"}
			</div>
		</header>
	);
}
