import { Link } from "react-router-dom";
import { PlayerPortrait, TeamBadge } from "@/shared/components";
import { ArrowRight, Handshake } from "@/shared/icons";
import type { P2PDeal } from "../../types/models";
import styles from "./P2PBestDealsWidget.module.scss";

export interface P2PBestDealsWidgetProps {
	deals: P2PDeal[];
	className?: string;
}

export function P2PBestDealsWidget({
	deals,
	className,
}: P2PBestDealsWidgetProps) {
	const formatCredits = (val: number) => {
		return `${val.toLocaleString("es-ES", {
			minimumFractionDigits: 2,
			maximumFractionDigits: 2,
		})} ₢`;
	};

	return (
		<div className={`${styles.card} ${className ?? ""}`.trim()}>
			<header className={styles.header}>
				<div className={styles.titleGroup}>
					<span className={styles.headerIconTile} aria-hidden="true">
						<Handshake className={styles.headerIcon} />
					</span>
					<div className={styles.headerTitles}>
						<h2 className={styles.title}>Oportunidades P2P</h2>
						<span className={styles.subtitle}>
							Listados de la comunidad con descuento sobre cotización oficial
						</span>
					</div>
				</div>

				<Link to="/market/p2p" className={styles.viewAllLink}>
					<span>Ver Mercado P2P</span>
					<ArrowRight
						size={14}
						className={styles.linkArrow}
						aria-hidden="true"
					/>
				</Link>
			</header>

			<div className={styles.dealsList}>
				{deals.length === 0 ? (
					<div className={styles.emptyState}>
						<p>No hay listados P2P activos con descuento en este momento.</p>
					</div>
				) : (
					deals.map((deal) => (
						<article key={deal.listingId} className={styles.dealItem}>
							<div className={styles.playerCol}>
								<PlayerPortrait
									src={deal.imageUrl}
									name={deal.playerName}
									size="sm"
									className={styles.portrait}
								/>
								<div className={styles.playerInfo}>
									<span className={styles.playerName}>{deal.playerName}</span>
									<span className={styles.playerTeam}>
										<TeamBadge
											teamName={deal.team}
											leagueName={deal.league}
											className={styles.teamShield}
										/>
										{deal.team}
									</span>
								</div>
							</div>

							<div className={styles.priceCol}>
								<div className={styles.priceRow}>
									<span className={styles.p2pPrice}>
										{formatCredits(deal.p2pUnitPrice)}
									</span>
									<span className={styles.officialPrice}>
										{formatCredits(deal.officialQuotePrice)}
									</span>
								</div>
								<span className={styles.tokensAvailable}>
									{deal.availableTokens} disp.
								</span>
							</div>

							<div className={styles.discountCol}>
								<span className={styles.discountBadge}>
									-{deal.discountPercentage.toFixed(1)}%
								</span>
							</div>

							<div className={styles.actionCol}>
								<Link to="/market/p2p" className={styles.buyBtn}>
									Comprar
								</Link>
							</div>
						</article>
					))
				)}
			</div>
		</div>
	);
}
