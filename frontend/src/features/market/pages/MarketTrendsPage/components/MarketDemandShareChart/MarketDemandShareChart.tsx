import { Link } from "react-router-dom";
import type { TopHotPlayer } from "@/features/market/types";
import type { Player } from "@/features/players/types";
import {
	NationalityFlag,
	PlayerPortrait,
	TokenSymbol,
} from "@/shared/components";
import { Flame } from "@/shared/icons";
import styles from "./MarketDemandShareChart.module.scss";

export interface MarketDemandShareChartProps {
	topHotPlayers: TopHotPlayer[];
	totalTradedTokens: number;
	playerMap?: Map<number, Player>;
}

export default function MarketDemandShareChart({
	topHotPlayers,
	totalTradedTokens,
	playerMap = new Map(),
}: MarketDemandShareChartProps) {
	if (!topHotPlayers.length) {
		return null;
	}

	const maxTokens = Math.max(...topHotPlayers.map((p) => p.boughtTokens), 1);

	return (
		<div className={styles.chartCard}>
			<div className={styles.chartHeader}>
				<div className={styles.titleGroup}>
					<Flame className={styles.flameIcon} aria-hidden="true" />
					<div>
						<h3 className={styles.title}>Cuota de Demanda (Top 5)</h3>
						<p className={styles.subtitle}>
							Distribución de tokens adquiridos respecto al volumen total
							negociado
						</p>
					</div>
				</div>
			</div>

			<div className={styles.barsList}>
				{topHotPlayers.map((player, index) => {
					const playerInfo = playerMap.get(player.playerId);
					const sharePercent =
						totalTradedTokens > 0
							? (player.boughtTokens / totalTradedTokens) * 100
							: 0;
					const visualWidth = (player.boughtTokens / maxTokens) * 100;

					return (
						<div key={player.playerId} className={styles.barItem}>
							<div className={styles.barItem__meta}>
								<div className={styles.barItem__player}>
									<span className={styles.barItem__rank}>#{index + 1}</span>

									<div className={styles.barItem__avatarWrap}>
										<PlayerPortrait
											src={playerInfo?.imageUrl}
											name={player.playerName}
											size="sm"
											className={styles.barItem__avatar}
										/>
										{playerInfo?.nationality && (
											<NationalityFlag
												nationality={playerInfo.nationality}
												className={styles.barItem__flag}
											/>
										)}
									</div>

									<div className={styles.barItem__nameGroup}>
										<Link
											to={`/players/${player.playerId}`}
											className={styles.barItem__name}
											title={`Ver detalles de ${player.playerName}`}
										>
											{player.playerName}
										</Link>
										<span className={styles.barItem__team}>
											{playerInfo?.teamName ?? "Sin equipo"}
										</span>
									</div>
								</div>

								<div className={styles.barItem__stats}>
									<span className={styles.barItem__tokens}>
										{player.boughtTokens.toLocaleString("es-AR")}{" "}
										<TokenSymbol size="xs" />
									</span>
									<span className={styles.barItem__volume}>
										(
										{player.buyVolumeCredits.toLocaleString("es-AR", {
											minimumFractionDigits: 2,
											maximumFractionDigits: 2,
										})}{" "}
										₵)
									</span>
									<span className={styles.barItem__shareBadge}>
										{sharePercent.toFixed(1)}% cuota
									</span>
								</div>
							</div>

							<div
								className={styles.barItem__track}
								role="progressbar"
								aria-label={`Cuota de demanda de ${player.playerName}`}
								aria-valuenow={Math.round(sharePercent)}
								aria-valuemin={0}
								aria-valuemax={100}
							>
								<div
									className={styles.barItem__fill}
									style={{ width: `${Math.max(6, visualWidth)}%` }}
								/>
							</div>
						</div>
					);
				})}
			</div>
		</div>
	);
}
