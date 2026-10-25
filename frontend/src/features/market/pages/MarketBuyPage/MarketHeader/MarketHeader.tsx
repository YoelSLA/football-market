import { Clock, Layers3, ShoppingCart, Wallet } from "@/shared/icons";
import styles from "./MarketHeader.module.scss";

interface Props {
	totalPlayers?: number;
}

export default function MarketHeader({ totalPlayers }: Props) {
	return (
		<header className={styles.marketHeader}>
			<div className={styles.marketHeader__heading}>
				<span className={styles.marketHeader__symbol}>
					<ShoppingCart aria-hidden="true" />
				</span>
				<div>
					<h1>Comprar tokens</h1>
					<p>Explorá el mercado y adquirí tokens de futbolistas.</p>
				</div>
			</div>
			<div className={styles.marketHeader__metrics}>
				<div className={styles.marketHeader__metric}>
					<span className={styles.marketHeader__tile}>
						<Wallet aria-hidden="true" />
					</span>
					<div>
						<p>Saldo disponible</p>
						<strong>No disponible</strong>
					</div>
				</div>
				<div
					className={`${styles.marketHeader__metric} ${styles.marketHeader__metric_violet}`}
				>
					<span className={styles.marketHeader__tile}>
						<Layers3 aria-hidden="true" />
					</span>
					<div>
						<p>Jugadores disponibles</p>
						<strong>
							{totalPlayers?.toLocaleString("es-AR") ?? "No disponible"}
						</strong>
					</div>
				</div>
				<div
					className={`${styles.marketHeader__metric} ${styles.marketHeader__metric_gold}`}
				>
					<span className={styles.marketHeader__tile}>
						<Clock aria-hidden="true" />
					</span>
					<div>
						<p>Órdenes pendientes</p>
						<strong>No disponible</strong>
					</div>
				</div>
			</div>
		</header>
	);
}
