import { useState } from "react";
import { Link } from "react-router-dom";
import { PlayerPortrait, TeamBadge } from "@/shared/components";
import { Flame, TrendingDown, TrendingUp } from "@/shared/icons";
import type { PlayerMover } from "../../types/models";
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

	const formatPrice = (price: number) => {
		return `${price.toLocaleString("es-ES", {
			minimumFractionDigits: 2,
			maximumFractionDigits: 2,
		})} ₢`;
	};

	const formatDeltaPct = (pct: number | null) => {
		if (pct === null) return "—";
		const sign = pct > 0 ? "+" : "";
		return `${sign}${pct.toFixed(2)}%`;
	};

	const formatDeltaCredits = (delta: number | null) => {
		if (delta === null) return "—";
		const sign = delta > 0 ? "+" : "";
		return `${sign}${delta.toFixed(2)} ₢`;
	};

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
							{movers.map((mover, idx) => {
								const isGainer = (mover.deltaPercentage ?? 0) >= 0;
								return (
									<tr key={mover.playerId} className={styles.row}>
										<td className={styles.tdRank}>{idx + 1}</td>
										<td className={styles.tdPlayer}>
											<Link
												to={`/players/${mover.playerId}`}
												className={styles.playerLink}
											>
												<PlayerPortrait
													src={mover.imageUrl}
													name={mover.name}
													size="sm"
													className={styles.portrait}
												/>
												<div className={styles.playerInfo}>
													<span className={styles.playerName}>
														{mover.name}
													</span>
													<span className={styles.playerMeta}>
														<TeamBadge
															teamName={mover.team}
															leagueName={mover.league}
															className={styles.teamBadge}
														/>
														{mover.team} · {mover.position}
													</span>
												</div>
											</Link>
										</td>
										<td className={styles.tdPrice}>
											{formatPrice(mover.currentPrice)}
										</td>
										<td className={styles.tdDeltaPct}>
											<span
												className={`${styles.deltaBadge} ${isGainer ? styles.gainBadge : styles.lossBadge}`}
											>
												{formatDeltaPct(mover.deltaPercentage)}
											</span>
										</td>
										<td
											className={`${styles.tdDeltaCredits} ${isGainer ? styles.gainText : styles.lossText}`}
										>
											{formatDeltaCredits(mover.deltaCredits)}
										</td>
									</tr>
								);
							})}
						</tbody>
					</table>
				)}
			</div>
		</div>
	);
}
