import { PageHeading } from "@/shared/components";
import { UsersRound, Wallet } from "@/shared/icons";
import styles from "./MarketP2PHeader.module.scss";

interface Props {
	availableCredits?: number;
}

export default function MarketP2PHeader({ availableCredits }: Props) {
	return (
		<header className={styles.marketP2PHeader}>
			<PageHeading
				title="Mercado P2P"
				description="Compará publicaciones y comprá lotes completos de otros usuarios."
				icon={UsersRound}
			/>
			<div className={styles.marketP2PHeader__metrics}>
				<div className={styles.marketP2PHeader__metric}>
					<span className={styles.marketP2PHeader__tile}>
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
