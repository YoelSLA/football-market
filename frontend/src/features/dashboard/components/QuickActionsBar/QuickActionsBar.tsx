import { Link } from "react-router-dom";
import { BarChart3, Handshake, Tag, Zap } from "@/shared/icons";
import styles from "./QuickActionsBar.module.scss";

export interface QuickActionsBarProps {
	className?: string;
}

export function QuickActionsBar({ className }: QuickActionsBarProps) {
	return (
		<nav
			className={`${styles.bar} ${className ?? ""}`.trim()}
			aria-label="Acciones rápidas del mercado"
		>
			<span className={styles.barLabel}>Acciones Rápidas:</span>

			<div className={styles.actionsList}>
				<Link
					to="/market/buy"
					className={`${styles.actionBtn} ${styles.btnPrimary}`}
				>
					<Zap className={styles.icon} aria-hidden="true" />
					<span>Comprar Tokens</span>
				</Link>

				<Link to="/market/p2p" className={styles.actionBtn}>
					<Handshake className={styles.icon} aria-hidden="true" />
					<span>Mercado P2P</span>
				</Link>

				<Link to="/market/sell" className={styles.actionBtn}>
					<Tag className={styles.icon} aria-hidden="true" />
					<span>Vender Tokens</span>
				</Link>

				<Link to="/portfolio" className={styles.actionBtn}>
					<BarChart3 className={styles.icon} aria-hidden="true" />
					<span>Mi Cartera</span>
				</Link>
			</div>
		</nav>
	);
}
