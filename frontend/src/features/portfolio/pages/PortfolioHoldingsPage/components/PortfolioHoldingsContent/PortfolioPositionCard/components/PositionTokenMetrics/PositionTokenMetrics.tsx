import { CircleDot, Coins, Lock } from "@/shared/icons";
import styles from "./PositionTokenMetrics.module.scss";

interface Props {
	quantity: number;
	availableQuantity: number;
	reservedQuantity: number;
}

export default function PositionTokenMetrics({
	quantity,
	availableQuantity,
	reservedQuantity,
}: Props) {
	const metrics = [
		{
			key: "tokens",
			iconClass: styles.iconWrapper_tokens,
			icon: Coins,
			label: "Tokens totales",
			value: String(quantity),
		},
		{
			key: "available",
			iconClass: styles.iconWrapper_available,
			icon: CircleDot,
			label: "Disponibles",
			value: String(availableQuantity),
		},
		{
			key: "reserved",
			iconClass: styles.iconWrapper_reserved,
			icon: Lock,
			label: "Reservados",
			value: String(reservedQuantity),
		},
	] as const;

	return (
		<section className={styles.tokenMetrics} aria-label="Métricas de tokens">
			{metrics.map(({ key, iconClass, icon: Icon, label, value }) => (
				<div key={key} className={styles.metric}>
					<span className={`${styles.iconWrapper} ${iconClass}`}>
						<Icon className={styles.icon} aria-hidden="true" />
					</span>
					<div className={styles.info}>
						<dt className={styles.value}>{value}</dt>
						<dd className={styles.label}>{label}</dd>
					</div>
				</div>
			))}
		</section>
	);
}
