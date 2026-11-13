import { PageHeading } from "@/shared/components";
import { Clock, Layers3, UsersRound, Wallet } from "@/shared/icons";
import styles from "./MarketP2PHeader.module.scss";

interface Props {
	totalPlayers?: number;
	availableCredits?: number;
}

export default function MarketP2PHeader({
	totalPlayers,
	availableCredits,
}: Props) {
	return (
		<header className={styles.marketP2PHeader}>
			<PageHeading
				title="Mercado P2P"
				description="Explorá publicaciones y comprá tokens de otros usuarios."
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
				<div
					className={`${styles.marketP2PHeader__metric} ${styles.marketP2PHeader__metric_violet}`}
				>
					<span className={styles.marketP2PHeader__tile}>
						<Layers3 aria-hidden="true" />
					</span>
					<div>
						<p>Jugadores con ofertas</p>
						<strong>
							{totalPlayers?.toLocaleString("es-AR") ?? "No disponible"}
						</strong>
					</div>
				</div>
				<div
					className={`${styles.marketP2PHeader__metric} ${styles.marketP2PHeader__metric_gold}`}
				>
					<span className={styles.marketP2PHeader__tile}>
						<Clock aria-hidden="true" />
					</span>
					<div>
						<p>Órdenes pendientes</p>
						<strong>No disponible</strong>
					</div>
				</div>
			</div>
		</header>
	);
}
