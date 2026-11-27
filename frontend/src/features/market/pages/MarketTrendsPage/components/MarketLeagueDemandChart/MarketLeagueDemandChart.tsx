import { useMemo } from "react";
import type { TopHotPlayer } from "@/features/market/types";
import type { Player } from "@/features/players/types";
import { TokenSymbol } from "@/shared/components";
import { Trophy } from "@/shared/icons";
import styles from "./MarketLeagueDemandChart.module.scss";

export interface MarketLeagueDemandChartProps {
	topHotPlayers: TopHotPlayer[];
	playerMap: Map<number, Player>;
}

interface LeagueStat {
	leagueName: string;
	displayName: string;
	tokens: number;
	volumeCredits: number;
	percentage: number;
	playerCount: number;
	color: string;
}

const LEAGUE_CONFIG: {
	key: string;
	label: string;
	color: string;
	matches: (name: string) => boolean;
}[] = [
	{
		key: "premier",
		label: "Premier League",
		color: "#8b5cf6",
		matches: (n) => n.toLowerCase().includes("premier"),
	},
	{
		key: "laliga",
		label: "LaLiga",
		color: "#ea580c",
		matches: (n) =>
			n.toLowerCase().includes("primera") || n.toLowerCase().includes("liga"),
	},
	{
		key: "seriea",
		label: "Serie A",
		color: "#0284c7",
		matches: (n) => n.toLowerCase().includes("serie a"),
	},
	{
		key: "bundesliga",
		label: "Bundesliga",
		color: "#dc2626",
		matches: (n) => n.toLowerCase().includes("bundesliga"),
	},
	{
		key: "ligue1",
		label: "Ligue 1",
		color: "#10b981",
		matches: (n) => n.toLowerCase().includes("ligue 1"),
	},
];

export default function MarketLeagueDemandChart({
	topHotPlayers,
	playerMap,
}: MarketLeagueDemandChartProps) {
	const stats: LeagueStat[] = useMemo(() => {
		const groups: Record<
			string,
			{ tokens: number; volume: number; count: number }
		> = {
			premier: { tokens: 0, volume: 0, count: 0 },
			laliga: { tokens: 0, volume: 0, count: 0 },
			seriea: { tokens: 0, volume: 0, count: 0 },
			bundesliga: { tokens: 0, volume: 0, count: 0 },
			ligue1: { tokens: 0, volume: 0, count: 0 },
		};

		let totalTokens = 0;

		for (const p of topHotPlayers) {
			const playerInfo = playerMap.get(p.playerId);
			const league = playerInfo?.leagueName ?? "";

			const matchedConfig = LEAGUE_CONFIG.find((cfg) => cfg.matches(league));
			const key = matchedConfig ? matchedConfig.key : "premier";

			groups[key].tokens += p.boughtTokens;
			groups[key].volume += p.buyVolumeCredits;
			groups[key].count += 1;
			totalTokens += p.boughtTokens;
		}

		return LEAGUE_CONFIG.map((cfg) => {
			const data = groups[cfg.key] ?? { tokens: 0, volume: 0, count: 0 };
			return {
				leagueName: cfg.key,
				displayName: cfg.label,
				tokens: data.tokens,
				volumeCredits: data.volume,
				percentage: totalTokens > 0 ? (data.tokens / totalTokens) * 100 : 0,
				playerCount: data.count,
				color: cfg.color,
			};
		});
	}, [topHotPlayers, playerMap]);

	return (
		<div className={styles.card}>
			<div className={styles.header}>
				<div className={styles.titleGroup}>
					<Trophy className={styles.icon} aria-hidden="true" />
					<div>
						<h3 className={styles.title}>Flujo por Liga</h3>
						<p className={styles.subtitle}>
							Destino del capital de compra en el Top 5
						</p>
					</div>
				</div>
			</div>

			<div className={styles.list}>
				{stats.map((item) => (
					<div key={item.leagueName} className={styles.item}>
						<div className={styles.itemMeta}>
							<div className={styles.itemLeague}>
								<span
									className={styles.colorDot}
									style={{ backgroundColor: item.color }}
									aria-hidden="true"
								/>
								<span className={styles.itemLabel}>{item.displayName}</span>
								{item.playerCount > 0 && (
									<span className={styles.itemCount}>
										({item.playerCount} en Top 5)
									</span>
								)}
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
