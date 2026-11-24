import { useState } from "react";
import { Flame, TrendingDown, TrendingUp } from "@/shared/icons";
import type { PlayerMover } from "../../types/models";
import { MarketMoversRow } from "./MarketMoversRow";
import styles from "./MarketMoversTable.module.scss";

export interface MarketMoversTableProps {
	topGainers: PlayerMover[];
	topLosers: PlayerMover[];
	className?: string;
}

export function MarketMoversTable({
	topGainers,
	topLosers,
	className,
}: MarketMoversTableProps) {
	const [activeTab, setActiveTab] = useState<"gainers" | "losers">("gainers");
	const movers = activeTab === "gainers" ? topGainers : topLosers;

	return (
		<div className={`${styles.card} ${className ?? ""}`.trim()}>
			<header className={styles.header}>
				<div className={styles.titleGroup}>
					<span className={styles.headerIconTile} aria-hidden="true">
						<Flame className={styles.headerIcon} />
					</span>
					<div className={styles.headerTitles}>
						<h2 className={styles.title}>Market Movers</h2>
						<span className={styles.subtitle}>
							Jugadores con mayor volatilidad en 24h
						</span>
					</div>
				</div>

				<div
					className={styles.tabGroup}
					role="tablist"
					aria-label="Filtro de movimientos de mercado"
				>
					<button
						type="button"
						role="tab"
						aria-selected={activeTab === "gainers"}
						className={`${styles.tabButton} ${activeTab === "gainers" ? styles.tabGainersActive : ""}`}
						onClick={() => setActiveTab("gainers")}
					>
						<TrendingUp
							size={13}
							className={styles.tabIcon}
							aria-hidden="true"
						/>
						<span>Top Subidas</span>
					</button>
					<button
						type="button"
						role="tab"
						aria-selected={activeTab === "losers"}
						className={`${styles.tabButton} ${activeTab === "losers" ? styles.tabLosersActive : ""}`}
						onClick={() => setActiveTab("losers")}
					>
						<TrendingDown
							size={13}
							className={styles.tabIcon}
							aria-hidden="true"
						/>
						<span>Top Caídas</span>
					</button>
				</div>
			</header>

			<div className={styles.tableWrapper}>
				{movers.length === 0 ? (
					<div className={styles.emptyState}>
						<p>No se registran variaciones significativas hoy.</p>
					</div>
				) : (
					<table className={styles.table}>
						<thead>
							<tr>
								<th className={styles.thRank}>#</th>
								<th className={styles.thPlayer}>Futbolista</th>
								<th className={styles.thPrice}>Precio</th>
								<th className={styles.thDeltaPct}>Var. 24h</th>
								<th className={styles.thDeltaCredits}>Var. Créditos</th>
							</tr>
						</thead>
						<tbody>
							{movers.map((mover, idx) => (
								<MarketMoversRow
									key={mover.playerId}
									mover={mover}
									rank={idx + 1}
								/>
							))}
						</tbody>
					</table>
				)}
			</div>
		</div>
	);
}
