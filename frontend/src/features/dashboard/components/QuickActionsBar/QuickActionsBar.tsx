import { Link } from "react-router-dom";
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
			<Link
				to="/market/buy"
				className={`${styles.actionBtn} ${styles.btnPrimary}`}
			>
				<span className={styles.icon} aria-hidden="true">
					⚡
				</span>
				<div className={styles.labelGroup}>
					<span className={styles.title}>Comprar Tokens</span>
					<span className={styles.desc}>Mercado Primario Oficial</span>
				</div>
			</Link>

			<Link to="/market/p2p" className={styles.actionBtn}>
				<span className={styles.icon} aria-hidden="true">
					🤝
				</span>
				<div className={styles.labelGroup}>
					<span className={styles.title}>Mercado P2P</span>
					<span className={styles.desc}>Ofertas de la Comunidad</span>
				</div>
			</Link>

			<Link to="/market/sell" className={styles.actionBtn}>
				<span className={styles.icon} aria-hidden="true">
					🏷️
				</span>
				<div className={styles.labelGroup}>
					<span className={styles.title}>Vender Tokens</span>
					<span className={styles.desc}>Publicar en P2P</span>
				</div>
			</Link>

			<Link to="/portfolio" className={styles.actionBtn}>
				<span className={styles.icon} aria-hidden="true">
					💼
				</span>
				<div className={styles.labelGroup}>
					<span className={styles.title}>Mi Cartera</span>
					<span className={styles.desc}>Gestión de Posiciones</span>
				</div>
			</Link>
		</nav>
	);
}
