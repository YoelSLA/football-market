import { Link } from "react-router-dom";
import { TokenSymbol } from "@/shared/components";
import { ArrowRight, History } from "@/shared/icons";
import type { RecentActivityItem } from "../../types/models";
import styles from "./LiveActivityFeed.module.scss";

export interface LiveActivityFeedProps {
	activities: RecentActivityItem[];
	className?: string;
}

export function LiveActivityFeed({
	activities,
	className,
}: LiveActivityFeedProps) {
	const formatCredits = (amount: number) => {
		return `${amount.toLocaleString("es-ES", {
			minimumFractionDigits: 2,
			maximumFractionDigits: 2,
		})} ₵`;
	};

	const formatDate = (isoString: string) => {
		try {
			const date = new Date(isoString);
			return date.toLocaleString("es-ES", {
				day: "2-digit",
				month: "2-digit",
				hour: "2-digit",
				minute: "2-digit",
			});
		} catch {
			return isoString;
		}
	};

	return (
		<div className={`${styles.card} ${className ?? ""}`.trim()}>
			<header className={styles.header}>
				<div className={styles.titleGroup}>
					<span className={styles.headerIconTile} aria-hidden="true">
						<History className={styles.headerIcon} />
					</span>
					<div className={styles.headerTitles}>
						<h2 className={styles.title}>Actividad Reciente</h2>
						<span className={styles.subtitle}>
							Últimas transacciones ejecutadas en tu cuenta
						</span>
					</div>
				</div>

				<Link to="/portfolio" className={styles.viewAllLink}>
					<span>Historial Completo</span>
					<ArrowRight
						size={14}
						className={styles.linkArrow}
						aria-hidden="true"
					/>
				</Link>
			</header>

			<div className={styles.feedList}>
				{activities.length === 0 ? (
					<div className={styles.emptyState}>
						<p>No has realizado transacciones recientemente.</p>
					</div>
				) : (
					activities.map((item) => {
						const isBuy = item.type === "BUY";
						return (
							<div key={item.id} className={styles.activityItem}>
								<div className={styles.typeBadgeCol}>
									<span
										className={`${styles.typeBadge} ${isBuy ? styles.buyBadge : styles.sellBadge}`}
									>
										{isBuy ? "COMPRA" : "VENTA"}
									</span>
									<span className={styles.marketBadge}>{item.marketType}</span>
								</div>

								<div className={styles.detailCol}>
									<div className={styles.playerRow}>
										<Link
											to={`/players/${item.playerId}`}
											className={styles.playerLink}
										>
											{item.tokens} <TokenSymbol size="xs" /> {item.playerName}
										</Link>
									</div>
									<span className={styles.unitPrice}>
										{formatCredits(item.unitPrice)} c/u
									</span>
								</div>

								<div className={styles.amountCol}>
									<span
										className={`${styles.totalAmount} ${isBuy ? styles.amountBuy : styles.amountSell}`}
									>
										{isBuy ? "-" : "+"}
										{formatCredits(item.totalAmount)}
									</span>
									<time className={styles.timestamp} dateTime={item.executedAt}>
										{formatDate(item.executedAt)}
									</time>
								</div>
							</div>
						);
					})
				)}
			</div>
		</div>
	);
}
