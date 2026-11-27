import { Link } from "react-router-dom";
import { TokenSymbol } from "@/shared/components";
import {
	Coins,
	Handshake,
	LayoutDashboard,
	TrendingUp,
	Zap,
} from "@/shared/icons";
import styles from "./DashboardEmptyState.module.scss";

export interface DashboardEmptyStateProps {
	availableCredits: number;
	className?: string;
}

export function DashboardEmptyState({
	availableCredits,
	className,
}: DashboardEmptyStateProps) {
	return (
		<div className={`${styles.emptyCard} ${className ?? ""}`.trim()}>
			<div className={styles.iconGlow} aria-hidden="true">
				<LayoutDashboard size={44} className={styles.emptyIcon} />
			</div>

			<h2 className={styles.title}>¡Bienvenido a Football Market!</h2>

			<p className={styles.description}>
				Aún no tienes posiciones abiertas en tu cartera. Tienes{" "}
				<strong className={styles.creditsHighlight}>
					{availableCredits.toLocaleString("es-ES", {
						minimumFractionDigits: 2,
						maximumFractionDigits: 2,
					})}{" "}
					₵
				</strong>{" "}
				de saldo disponible para armar tu plantilla de activos deportivos y
				comenzar a rentabilizar tus decisiones financieras.
			</p>

			<div className={styles.featuresGrid}>
				<div className={styles.featureItem}>
					<span className={styles.featureIcon} aria-hidden="true">
						<TrendingUp size={22} />
					</span>
					<span className={styles.featureTitle}>Mercado Oficial</span>
					<span className={styles.featureDesc}>
						Compra <TokenSymbol size="xs" /> directamente al club a precio de
						cotización.
					</span>
				</div>

				<div className={styles.featureItem}>
					<span className={styles.featureIcon} aria-hidden="true">
						<Handshake size={22} />
					</span>
					<span className={styles.featureTitle}>Comunidad P2P</span>
					<span className={styles.featureDesc}>
						Aprovecha descuentos y negocia con otros managers.
					</span>
				</div>

				<div className={styles.featureItem}>
					<span className={styles.featureIcon} aria-hidden="true">
						<Coins size={22} />
					</span>
					<span className={styles.featureTitle}>Dividendos Deportivos</span>
					<span className={styles.featureDesc}>
						Tus futbolistas revalorizan su cotización según su rendimiento real.
					</span>
				</div>
			</div>

			<div className={styles.actions}>
				<Link to="/market/buy" className={styles.primaryBtn}>
					<Zap size={15} aria-hidden="true" />
					<span>
						Comprar mis primeros <TokenSymbol size="sm" />
					</span>
				</Link>
				<Link to="/players" className={styles.secondaryBtn}>
					Explorar Catálogo
				</Link>
			</div>
		</div>
	);
}
