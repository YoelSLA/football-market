import { useMemo } from "react";
import type { TopHotPlayer } from "@/features/market/types";
import { PlayerPositionBadge } from "@/features/players";
import type { Player } from "@/features/players/types";
import { TokenSymbol } from "@/shared/components";
import { UsersRound } from "@/shared/icons";
import styles from "./MarketPositionalDemandChart.module.scss";

export interface MarketPositionalDemandChartProps {
	topHotPlayers: TopHotPlayer[];
	playerMap: Map<number, Player>;
}

interface PositionalStat {
	position: string;
	label: string;
	tokens: number;
	volumeCredits: number;
	percentage: number;
	playerCount: number;
	color: string;
}

const POSITION_NAMES: Record<string, string> = {
	DEL: "Delanteros",
	MED: "Centrocampistas",
	DEF: "Defensas",
	POR: "Porteros",
	ARQ: "Porteros",
};

const POSITION_COLORS: Record<string, string> = {
	DEL: "#ef4444",
	MED: "#8b5cf6",
	DEF: "#06b6d4",
	POR: "#f59e0b",
	ARQ: "#f59e0b",
};

export default function MarketPositionalDemandChart({
	topHotPlayers,
	playerMap,
}: MarketPositionalDemandChartProps) {
	const stats: PositionalStat[] = useMemo(() => {
		const groups: Record<
			string,
			{ tokens: number; volume: number; count: number }
		> = {
			DEL: { tokens: 0, volume: 0, count: 0 },
			MED: { tokens: 0, volume: 0, count: 0 },
			DEF: { tokens: 0, volume: 0, count: 0 },
			POR: { tokens: 0, volume: 0, count: 0 },
		};

		let totalTokens = 0;

		for (const p of topHotPlayers) {
			const playerInfo = playerMap.get(p.playerId);
			const rawPos = (playerInfo?.position ?? "").toUpperCase();
			const posKey =
				rawPos.includes("POR") ||
				rawPos.includes("ARQ") ||
				rawPos.includes("GOAL") ||
				rawPos.includes("GK")
					? "POR"
					: rawPos.includes("DEF")
						? "DEF"
						: rawPos.includes("MED") || rawPos.includes("MID")
							? "MED"
							: rawPos.includes("DEL") ||
									rawPos.includes("FOR") ||
									rawPos.includes("ATT") ||
									rawPos.includes("OFF")
								? "DEL"
								: "MED";

			groups[posKey].tokens += p.boughtTokens;
			groups[posKey].volume += p.buyVolumeCredits;
			groups[posKey].count += 1;
			totalTokens += p.boughtTokens;
		}

		if (totalTokens === 0) return [];

		return ["POR", "DEF", "MED", "DEL"].map((pos) => {
			const data = groups[pos] ?? { tokens: 0, volume: 0, count: 0 };
			return {
				position: pos,
				label: POSITION_NAMES[pos] ?? pos,
				tokens: data.tokens,
				volumeCredits: data.volume,
				percentage: totalTokens > 0 ? (data.tokens / totalTokens) * 100 : 0,
				playerCount: data.count,
				color: POSITION_COLORS[pos] ?? "#3b82f6",
			};
		});
	}, [topHotPlayers, playerMap]);

	if (!stats.length) return null;

	return (
		<div className={styles.card}>
			<div className={styles.header}>
				<div className={styles.titleGroup}>
					<UsersRound className={styles.icon} aria-hidden="true" />
					<div>
						<h3 className={styles.title}>Demanda por Demarcación Táctica</h3>
						<p className={styles.subtitle}>
							Distribución de tokens adquiridos según la posición del jugador
						</p>
					</div>
				</div>
			</div>

			<div className={styles.list}>
				{stats.map((item) => (
					<div key={item.position} className={styles.item}>
						<div className={styles.itemMeta}>
							<div className={styles.itemPos}>
								<PlayerPositionBadge position={item.position} />
								<span className={styles.itemLabel}>{item.label}</span>
								<span className={styles.itemCount}>
									({item.playerCount} en el Top 5)
								</span>
							</div>
							<div className={styles.itemStats}>
								<span className={styles.itemTokens}>
									<span className={styles.tokensNum}>
										{item.tokens.toLocaleString("es-AR")}
									</span>
									<TokenSymbol size="xs" />
								</span>
								<strong className={styles.itemPercent}>
									{item.percentage.toFixed(1)}%
								</strong>
							</div>
						</div>

						<div className={styles.track}>
							<div
								className={styles.fill}
								style={{
									width: `${item.percentage}%`,
									backgroundColor: item.color,
								}}
							/>
						</div>
					</div>
				))}
			</div>
		</div>
	);
}
