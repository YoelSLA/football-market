import type { ReactNode } from "react";
import { PageHeading } from "@/shared/components";
import { type LucideIcon, Wallet } from "@/shared/icons";
import styles from "./MarketPageHeader.module.scss";

export interface MarketHeaderMetricProps {
	icon: LucideIcon;
	label: string;
	value: ReactNode;
	title?: string;
}

export function MarketHeaderMetric({
	icon: Icon,
	label,
	value,
	title,
}: MarketHeaderMetricProps) {
	return (
		<div className={styles.marketPageHeader__metric} title={title}>
			<span className={styles.marketPageHeader__tile}>
				<Icon aria-hidden="true" />
			</span>
			<div>
				<p>{label}</p>
				<strong>{value}</strong>
			</div>
		</div>
	);
}

interface Props {
	title: string;
	description: string;
	icon?: LucideIcon | ReactNode;
	availableCredits?: number;
	children?: ReactNode;
}

export default function MarketPageHeader({
	title,
	description,
	icon,
	availableCredits,
	children,
}: Props) {
	return (
		<header className={styles.marketPageHeader}>
			<PageHeading title={title} description={description} icon={icon} />
			<div className={styles.marketPageHeader__metrics}>
				{children ?? (
					<MarketHeaderMetric
						icon={Wallet}
						label="Saldo disponible"
						value={
							availableCredits != null
								? `${availableCredits.toLocaleString("es-AR")} ₵`
								: "No disponible"
						}
					/>
				)}
			</div>
		</header>
	);
}
