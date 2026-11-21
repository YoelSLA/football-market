import { Link } from "react-router-dom";
import { Activity } from "@/shared/icons";
import type { MarketPulse, PlayerMover } from "../../types/models";
import styles from "./MarketTicker.module.scss";

export interface MarketTickerProps {
	movers: PlayerMover[];
	marketPulse?: MarketPulse;
	className?: string;
}

export function MarketTicker({
	movers,
	marketPulse,
	className,
}: MarketTickerProps) {
	if (!movers || movers.length === 0) return null;

	const tickerItems = [...movers, ...movers];

	const formatPrice = (price: number) => {
		return `${price.toLocaleString("es-ES", {
			minimumFractionDigits: 2,
			maximumFractionDigits: 2,
		})} ₢`;
	};

	const formatDelta = (pct: number | null) => {
		if (pct === null) return "—";
		const sign = pct > 0 ? "+" : "";
		return `${sign}${pct.toFixed(2)}%`;
	};

	return (
		<section
			className={`${styles.tickerBar} ${className ?? ""}`.trim()}
			aria-label="Cinta de cotizaciones en tiempo real"
		>
			<div className={styles.pulseSummary}>
				<span className={styles.pulseLiveDot} aria-hidden="true" />
				<Activity className={styles.pulseIcon} aria-hidden="true" />
				{marketPulse && (
					<>
						<span className={styles.pulseItem}>
							<span className={styles.pulseLabel}>CAP:</span>{" "}
							{marketPulse.totalMarketCap.toLocaleString("es-ES", {
								maximumFractionDigits: 0,
							})}{" "}
							₢
						</span>
						<span className={styles.pulseDivider}>|</span>
						<span className={styles.pulseItem}>
							<span className={styles.pulseLabel}>VOL 24H:</span>{" "}
							{marketPulse.volume24h.toLocaleString("es-ES", {
								maximumFractionDigits: 0,
							})}{" "}
							₢
						</span>
					</>
				)}
			</div>

			<div className={styles.marqueeTrackWrapper}>
				<div className={styles.marqueeTrack}>
					{tickerItems.map((item, idx) => {
						const isUp = (item.deltaPercentage ?? 0) >= 0;
						const trackInstance = idx < movers.length ? "primary" : "clone";
						return (
							<Link
								key={`${trackInstance}-${item.playerId}`}
								to={`/players/${item.playerId}`}
								className={styles.tickerChip}
							>
								<span className={styles.chipName}>{item.name}</span>
								<span className={styles.chipTeam}>({item.team})</span>
								<span className={styles.chipPrice}>
									{formatPrice(item.currentPrice)}
								</span>
								<span
									className={`${styles.chipDelta} ${isUp ? styles.chipDeltaUp : styles.chipDeltaDown}`}
								>
									{isUp ? "▲" : "▼"} {formatDelta(item.deltaPercentage)}
								</span>
							</Link>
						);
					})}
				</div>
			</div>
		</section>
	);
}
