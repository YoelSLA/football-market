import { Flame } from "@/shared/icons";
import styles from "./HotDemandBadge.module.scss";

export interface HotDemandBadgeProps {
	className?: string;
}

/**
 * Insignia visual "Alta Demanda" para activos presentes en el Top 5
 * de compras comunitarias durante las últimas 24 horas (SPEC-029).
 */
export default function HotDemandBadge({ className }: HotDemandBadgeProps) {
	return (
		<span
			className={`${styles.badge} ${className ?? ""}`.trim()}
			title="Activo en el Top 5 de demanda comunitaria en las últimas 24 horas"
			role="status"
			aria-label="Activo con alta demanda comunitaria"
		>
			<Flame className={styles.badge__icon} aria-hidden="true" />
			<span className={styles.badge__label}>Alta Demanda</span>
		</span>
	);
}
