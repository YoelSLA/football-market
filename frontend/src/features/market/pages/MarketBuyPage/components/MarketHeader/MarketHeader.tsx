import { PageHeading } from "@/shared/components";
import { ShoppingCart, Wallet } from "@/shared/icons";
import styles from "./MarketHeader.module.scss";

interface Props {
	availableCredits?: number;
}

export default function MarketHeader({ availableCredits }: Props) {
	return (
		<header className={styles.marketHeader}>
			<PageHeading
				title="Mercado Oficial"
				description="Comprá tokens directamente a la emisión oficial con la cotización vigente."
				icon={ShoppingCart}
			/>
			<div className={styles.marketHeader__metrics}>
				<div className={styles.marketHeader__metric}>
					<span className={styles.marketHeader__tile}>
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
