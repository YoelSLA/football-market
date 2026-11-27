import { Link } from "react-router-dom";
import { PlayerPortrait, TeamBadge } from "@/shared/components";
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
	marketPulse: _marketPulse,
	className,
}: MarketTickerProps) {
	if (!movers || movers.length === 0) return null;

	// Desduplicar movers por playerId para evitar claves redundantes
	const uniqueMovers = Array.from(
		new Map(movers.map((m) => [m.playerId, m])).values(),
	);

	// Duplicar elementos para garantizar cobertura continua y animación fluida sin saltos
	const tickerItems = [
		...uniqueMovers.map((m) => ({ ...m, instance: "a" })),
		...uniqueMovers.map((m) => ({ ...m, instance: "b" })),
		...uniqueMovers.map((m) => ({ ...m, instance: "c" })),
		...uniqueMovers.map((m) => ({ ...m, instance: "d" })),
	];

	const formatPrice = (price: number) => {
		return `${price.toLocaleString("es-ES", {
			minimumFractionDigits: 2,
			maximumFractionDigits: 2,
		})} ₵`;
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
				<span className={styles.pulseTitle}>EN VIVO</span>
			</div>

			<div className={styles.marqueeTrackWrapper}>
				<div className={styles.marqueeTrack}>
					{tickerItems.map((item) => {
						const isUp = (item.deltaPercentage ?? 0) >= 0;
						return (
							<Link
								key={`${item.instance}-${item.playerId}`}
								to={`/players/${item.playerId}`}
								className={styles.tickerChip}
							>
								<PlayerPortrait
									src={item.imageUrl}
									name={item.name}
									size="sm"
									fit="cover"
									className={styles.chipPortrait}
								/>
								<span className={styles.chipName}>{item.name}</span>
								<span className={styles.chipTeamGroup}>
									<TeamBadge
										teamName={item.team}
										leagueName={item.league}
										className={styles.teamBadge}
									/>
									<span className={styles.chipTeamName}>{item.team}</span>
								</span>
								<span className={styles.chipPrice}>
									{formatPrice(item.currentPrice)}
								</span>
								<span
									className={`${styles.chipDelta} ${
										isUp ? styles.chipDeltaUp : styles.chipDeltaDown
									}`}
								>
									<span className={styles.chipArrow} aria-hidden="true">
										{isUp ? "▲" : "▼"}
									</span>
									{formatDelta(item.deltaPercentage)}
								</span>
							</Link>
						);
					})}
				</div>
			</div>
		</section>
	);
}
