import type { ReactNode } from "react";
import { PageHeading } from "@/shared/components";
import { type LucideIcon, Wallet } from "@/shared/icons";
import styles from "./MarketPageHeader.module.scss";

interface Props {
	title: string;
	description: string;
	icon?: LucideIcon | ReactNode;
	availableCredits?: number;
}

export default function MarketPageHeader({
	title,
	description,
	icon,
	availableCredits,
}: Props) {
	return (
		<header className={styles.marketPageHeader}>
			<PageHeading title={title} description={description} icon={icon} />
			<div className={styles.marketPageHeader__metrics}>
				<div className={styles.marketPageHeader__metric}>
					<span className={styles.marketPageHeader__tile}>
						<Wallet aria-hidden="true" />
					</span>
					<div>
						<p>Saldo disponible</p>
						<strong>
							{availableCredits != null
								? `${availableCredits.toLocaleString("es-AR")} créditos`
								: "No disponible"}
						</strong>
					</div>
				</div>
			</div>
		</header>
	);
}
