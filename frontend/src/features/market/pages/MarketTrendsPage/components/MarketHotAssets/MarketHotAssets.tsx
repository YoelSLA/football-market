import { Link, useNavigate } from "react-router-dom";
import type { MarketPlayerQuoteState } from "@/features/market/hooks/queries";
import type {
	OfficialMarketPlayer,
	TopHotPlayer,
} from "@/features/market/types";
import { PlayerPositionBadge } from "@/features/players";
import { toTacticalPosition } from "@/features/players/players.mapper";
import type { Player, TacticalPosition } from "@/features/players/types";
import {
	NationalityFlag,
	PlayerPortrait,
	TokenSymbol,
} from "@/shared/components";
import { Flame, ShoppingCart } from "@/shared/icons";
import styles from "./MarketHotAssets.module.scss";

export interface MarketHotAssetsProps {
	topHotPlayers: TopHotPlayer[];
	totalTradedTokens: number;
	playerMap?: Map<number, Player>;
	quotesByPlayerId?: Map<number, MarketPlayerQuoteState>;
	onBuyClick?: (
		player: OfficialMarketPlayer,
		triggerEl: HTMLElement | null,
	) => void;
}

export default function MarketHotAssets({
	topHotPlayers,
	totalTradedTokens,
	playerMap = new Map(),
	quotesByPlayerId,
	onBuyClick,
}: MarketHotAssetsProps) {
	const navigate = useNavigate();

	if (!topHotPlayers.length) {
		return null;
	}

	const maxTokens = Math.max(...topHotPlayers.map((p) => p.boughtTokens), 1);

	const handleBuy = (
		player: TopHotPlayer,
		playerInfo: Player | undefined,
		e: React.MouseEvent<HTMLButtonElement>,
	) => {
		if (onBuyClick) {
			const quoteState = quotesByPlayerId?.get(player.playerId);
			const quote = quoteState?.quote;
			const officialPlayer: OfficialMarketPlayer = {
				id: player.playerId,
				name: player.playerName,
				position: (toTacticalPosition(playerInfo?.position) ??
					"DEL") as TacticalPosition,
				nationality: playerInfo?.nationality ?? null,
				teamId: playerInfo?.teamId ?? null,
				teamName: playerInfo?.teamName ?? null,
				teamBadgeUrl: null,
				leagueId: playerInfo?.leagueId ?? null,
				leagueName: playerInfo?.leagueName ?? null,
				imageUrl: playerInfo?.imageUrl ?? null,
				fallbackImageUrl: playerInfo?.fallbackImageUrl ?? null,
				quote: quote
					? {
							price: quote.price,
							previousPrice: quote.previousPrice ?? null,
							variationPercentage:
								quote.previousPrice != null && quote.previousPrice > 0
									? ((quote.price - quote.previousPrice) /
											quote.previousPrice) *
										100
									: null,
							effectiveDate: quote.effectiveDate,
						}
					: null,
			};
			onBuyClick(officialPlayer, e.currentTarget);
		} else {
			navigate(`/market/buy?search=${encodeURIComponent(player.playerName)}`);
		}
	};

	return (
		<section
			className={styles.section}
			aria-label="Activos en tendencia del mercado"
		>
			<div className={styles.sectionHeader}>
				<div className={styles.titleGroup}>
					<Flame className={styles.flameIcon} aria-hidden="true" />
					<div>
						<h3 className={styles.title}>Activos en Tendencia (Top 5)</h3>
						<p className={styles.subtitle}>
							Futbolistas con mayor presión compradora y cuota de demanda en 24h
						</p>
					</div>
				</div>
			</div>

			<div className={styles.tableWrap}>
				<table className={styles.table}>
					<thead>
						<tr>
							<th className={styles.thRank}>#</th>
							<th className={styles.thPlayer}>Jugador</th>
							<th className={styles.thPos}>Posición</th>
							<th className={styles.thTokens}>Tokens 24h</th>
							<th className={styles.thVolume}>Volumen</th>
							<th className={styles.thShare}>Cuota</th>
							<th className={styles.thAction}>Acción</th>
						</tr>
					</thead>
					<tbody>
						{topHotPlayers.map((player, index) => {
							const playerInfo = playerMap.get(player.playerId);
							const sharePercent =
								totalTradedTokens > 0
									? (player.boughtTokens / totalTradedTokens) * 100
									: 0;
							const visualWidth = (player.boughtTokens / maxTokens) * 100;

							return (
								<tr key={player.playerId} className={styles.row}>
									<td className={styles.tdRank}>
										<span
											className={`${styles.rankBadge} ${
												index === 0 ? styles.rankBadge_gold : ""
											}`}
										>
											{index === 0 ? "🔥 1" : `#${index + 1}`}
										</span>
									</td>

									<td className={styles.tdPlayer}>
										<div className={styles.playerMeta}>
											<div className={styles.avatarWrap}>
												<PlayerPortrait
													src={playerInfo?.imageUrl}
													name={player.playerName}
													size="sm"
													className={styles.avatar}
												/>
												{playerInfo?.nationality && (
													<NationalityFlag
														nationality={playerInfo.nationality}
														className={styles.flag}
													/>
												)}
											</div>
											<div className={styles.nameGroup}>
												<Link
													to={`/players/${player.playerId}`}
													className={styles.playerName}
													title={`Ver ficha de ${player.playerName}`}
												>
													{player.playerName}
												</Link>
												<span className={styles.playerTeam}>
													{playerInfo?.teamName ?? "Sin equipo"}
												</span>
											</div>
										</div>
									</td>

									<td className={styles.tdPos}>
										{playerInfo?.position && (
											<PlayerPositionBadge position={playerInfo.position} />
										)}
									</td>

									<td className={styles.tdTokens}>
										<span className={styles.tokensValue}>
											{player.boughtTokens.toLocaleString("es-AR")}{" "}
											<TokenSymbol size="xs" />
										</span>
									</td>

									<td className={styles.tdVolume}>
										<span className={styles.volumeValue}>
											{player.buyVolumeCredits.toLocaleString("es-AR", {
												minimumFractionDigits: 2,
												maximumFractionDigits: 2,
											})}{" "}
											₵
										</span>
									</td>

									<td className={styles.tdShare}>
										<div className={styles.shareGroup}>
											<span className={styles.shareText}>
												{sharePercent.toFixed(1)}%
											</span>
											<div
												className={styles.barTrack}
												role="progressbar"
												aria-valuenow={Math.round(sharePercent)}
												aria-valuemin={0}
												aria-valuemax={100}
											>
												<div
													className={styles.barFill}
													style={{ width: `${Math.max(6, visualWidth)}%` }}
												/>
											</div>
										</div>
									</td>

									<td className={styles.tdAction}>
										<button
											type="button"
											className={styles.buyButton}
											onClick={(e) => handleBuy(player, playerInfo, e)}
											aria-label={`Comprar tokens de ${player.playerName}`}
										>
											<ShoppingCart size={12} aria-hidden="true" />
											Comprar
										</button>
									</td>
								</tr>
							);
						})}
					</tbody>
				</table>
			</div>
		</section>
	);
}
