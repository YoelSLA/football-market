import { Link } from "react-router-dom";
import type { TopHotPlayer } from "@/features/market/types";
import { PlayerPositionBadge } from "@/features/players";
import type { Player } from "@/features/players/types";
import {
	NationalityFlag,
	PlayerPortrait,
	TokenSymbol,
} from "@/shared/components";
import { ArrowRight, Flame } from "@/shared/icons";
import styles from "./MarketHotAssets.module.scss";

export interface MarketHotAssetsProps {
	topHotPlayers: TopHotPlayer[];
	playerMap?: Map<number, Player>;
}

export default function MarketHotAssets({
	topHotPlayers,
	playerMap = new Map(),
}: MarketHotAssetsProps) {
	if (!topHotPlayers.length) {
		return null;
	}

	return (
		<section className={styles.section}>
			<div className={styles.sectionHeader}>
				<div className={styles.titleGroup}>
					<Flame className={styles.flameIcon} aria-hidden="true" />
					<div>
						<h2 className={styles.title}>Activos en Tendencia (Top 5)</h2>
						<p className={styles.subtitle}>
							Jugadores con mayor presión compradora acumulada en las últimas 24
							horas
						</p>
					</div>
				</div>
			</div>

			<div className={styles.grid}>
				{topHotPlayers.map((player, index) => {
					const playerInfo = playerMap.get(player.playerId);

					return (
						<div key={player.playerId} className={styles.playerCard}>
							<div className={styles.playerCard__top}>
								<div className={styles.playerCard__rankBadge}>#{index + 1}</div>
								<div className={styles.playerCard__badges}>
									{playerInfo?.position && (
										<PlayerPositionBadge position={playerInfo.position} />
									)}
									<div
										className={styles.playerCard__hotBadge}
										title="Activo en el Top 5 de demanda comunitaria"
									>
										<Flame
											className={styles.playerCard__flameIcon}
											aria-hidden="true"
										/>
										<span>Alta Demanda</span>
									</div>
								</div>
							</div>

							<div className={styles.playerCard__identity}>
								<div className={styles.playerCard__portraitWrapper}>
									<PlayerPortrait
										src={playerInfo?.imageUrl}
										name={player.playerName}
										size="md"
										className={styles.playerCard__portrait}
									/>
									{playerInfo?.nationality && (
										<NationalityFlag
											nationality={playerInfo.nationality}
											className={styles.playerCard__flag}
										/>
									)}
								</div>

								<div className={styles.playerCard__meta}>
									<h3 className={styles.playerCard__name}>
										<Link
											to={`/players/${player.playerId}`}
											className={styles.playerCard__link}
											title={`Ver ficha técnica y cotización de ${player.playerName}`}
										>
											{player.playerName}
										</Link>
									</h3>
									<span className={styles.playerCard__clubLeague}>
										{playerInfo?.teamName ?? "Sin equipo"} ·{" "}
										{playerInfo?.leagueName ?? "Sin liga"}
									</span>
								</div>
							</div>

							<div className={styles.playerCard__stats}>
								<div className={styles.statRow}>
									<span className={styles.statLabel}>Tokens comprados:</span>
									<strong className={styles.statValue}>
										{player.boughtTokens.toLocaleString("es-AR")}{" "}
										<TokenSymbol size="xs" />
									</strong>
								</div>
								<div className={styles.statRow}>
									<span className={styles.statLabel}>Volumen comprador:</span>
									<strong className={styles.statValueAccent}>
										{player.buyVolumeCredits.toLocaleString("es-AR", {
											minimumFractionDigits: 2,
											maximumFractionDigits: 2,
										})}{" "}
										₵
									</strong>
								</div>
								<div className={styles.statRow}>
									<span className={styles.statLabel}>Cuota de demanda:</span>
									<strong className={styles.statValueShare}>
										{player.sharePercentage}%
									</strong>
								</div>
							</div>

							<div className={styles.playerCard__actions}>
								<Link
									to={`/players/${player.playerId}`}
									className={styles.actionButton}
									title={`Ver ficha técnica y cotización de ${player.playerName}`}
								>
									<span>Ver cotización</span>
									<ArrowRight
										className={styles.actionButton__icon}
										aria-hidden="true"
									/>
								</Link>
							</div>
						</div>
					);
				})}
			</div>
		</section>
	);
}
