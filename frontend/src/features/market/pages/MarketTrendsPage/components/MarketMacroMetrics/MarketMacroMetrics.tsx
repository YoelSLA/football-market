import { TokenSymbol } from "@/shared/components";
import { Activity, Coins, Flame } from "@/shared/icons";
import styles from "./MarketMacroMetrics.module.scss";

export interface MarketMacroMetricsProps {
	totalTradedCredits: number;
	totalTradedTokens: number;
	totalTransactions: number;
	topPlayerName?: string;
}

export default function MarketMacroMetrics({
	totalTradedCredits,
	totalTradedTokens,
	totalTransactions,
	topPlayerName,
}: MarketMacroMetricsProps) {
	return (
		<div className={styles.grid}>
			<div className={styles.card}>
				<div className={`${styles.iconTile} ${styles["iconTile--blue"]}`}>
					<Coins className={styles.icon} aria-hidden="true" />
				</div>
				<div className={styles.content}>
					<span className={styles.label}>Volumen Negociado</span>
					<strong className={styles.value}>
						{totalTradedCredits.toLocaleString("es-AR")} ₵
					</strong>
					<span className={styles.subtext}>
						{totalTradedTokens.toLocaleString("es-AR")}{" "}
						<TokenSymbol size="xs" /> negociados
					</span>
				</div>
			</div>

			<div className={styles.card}>
				<div className={`${styles.iconTile} ${styles["iconTile--purple"]}`}>
					<Activity className={styles.icon} aria-hidden="true" />
				</div>
				<div className={styles.content}>
					<span className={styles.label}>Operaciones Totales</span>
					<strong className={styles.value}>
						{totalTransactions.toLocaleString("es-AR")}
					</strong>
					<span className={styles.subtext}>
						Órdenes de compra y venta ejecutadas
					</span>
				</div>
			</div>

			<div className={styles.card}>
				<div className={`${styles.iconTile} ${styles["iconTile--amber"]}`}>
					<Flame className={styles.icon} aria-hidden="true" />
				</div>
				<div className={styles.content}>
					<span className={styles.label}>Activo Más Demandado</span>
					<strong className={styles.value}>
						{topPlayerName ?? "Sin operaciones"}
					</strong>
					<span className={styles.subtext}>
						Líder en volumen comprador (24h)
					</span>
				</div>
			</div>
		</div>
	);
}
