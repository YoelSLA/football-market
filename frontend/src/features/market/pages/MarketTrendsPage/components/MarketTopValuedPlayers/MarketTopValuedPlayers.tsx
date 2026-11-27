import { useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";
import type { MarketPlayerQuoteState } from "@/features/market/hooks/queries";
import type { OfficialMarketPlayer } from "@/features/market/types";
import {
	PlayerPositionBadge,
	usePlayerQuoteRankingQuery,
} from "@/features/players";
import {
	toQuoteRankingItems,
	toTacticalPosition,
} from "@/features/players/players.mapper";
import type {
	Player,
	PlayerRankingItem,
	TacticalPosition,
} from "@/features/players/types";
import { PlayerPortrait } from "@/shared/components";
import { ShoppingCart, TrendingUp } from "@/shared/icons";
import styles from "./MarketTopValuedPlayers.module.scss";

export interface MarketTopValuedPlayersProps {
	playerMap?: Map<number, Player>;
	quotesByPlayerId?: Map<number, MarketPlayerQuoteState>;
	onBuyClick?: (
		player: OfficialMarketPlayer,
		triggerEl: HTMLElement | null,
	) => void;
}

export default function MarketTopValuedPlayers({
	playerMap,
	quotesByPlayerId,
	onBuyClick,
}: MarketTopValuedPlayersProps = {}) {
	const navigate = useNavigate();
	const { data, isLoading } = usePlayerQuoteRankingQuery({
		page: 0,
		size: 5,
		order: "desc",
	});

	const items = useMemo(() => {
		if (!data) return [];
		return toQuoteRankingItems(data);
	}, [data]);

	const handleBuy = (
		player: PlayerRankingItem,
		e: React.MouseEvent<HTMLButtonElement>,
	) => {
		if (onBuyClick) {
			const playerInfo = playerMap?.get(player.playerId);
			const quoteState = quotesByPlayerId?.get(player.playerId);
			const quote = quoteState?.quote;
			const priceCentimes =
				quote?.price ?? Math.round((player.price ?? 0) * 100);

			const officialPlayer: OfficialMarketPlayer = {
				id: player.playerId,
				name: player.playerName,
				position: (toTacticalPosition(player.tacticalPosition) ??
					toTacticalPosition(playerInfo?.position) ??
					"MED") as TacticalPosition,
				nationality: playerInfo?.nationality ?? null,
				teamId: playerInfo?.teamId ?? null,
				teamName: player.teamName ?? playerInfo?.teamName ?? null,
				teamBadgeUrl: player.teamBadge ?? null,
				leagueId: playerInfo?.leagueId ?? null,
				leagueName: player.leagueName ?? playerInfo?.leagueName ?? null,
				imageUrl: player.playerAvatar ?? playerInfo?.imageUrl ?? null,
				fallbackImageUrl: playerInfo?.fallbackImageUrl ?? null,
				quote: {
					price: priceCentimes,
					previousPrice: quote?.previousPrice ?? null,
					variationPercentage:
						quote?.previousPrice != null && quote.previousPrice > 0
							? ((priceCentimes - quote.previousPrice) / quote.previousPrice) *
								100
							: null,
					effectiveDate:
						quote?.effectiveDate ??
						player.formattedDate ??
						new Date().toISOString().slice(0, 10),
				},
			};
			onBuyClick(officialPlayer, e.currentTarget);
		} else {
			navigate(`/market/buy?search=${encodeURIComponent(player.playerName)}`);
		}
	};

	if (isLoading || !items.length) {
		return null;
	}

	return (
		<section
			className={styles.section}
			aria-label="Mayores cotizaciones del mercado"
		>
			<div className={styles.sectionHeader}>
				<div className={styles.titleGroup}>
					<TrendingUp className={styles.icon} aria-hidden="true" />
					<div>
						<h3 className={styles.title}>Mayores Cotizaciones (Top 5)</h3>
						<p className={styles.subtitle}>
							Líderes de valoración en el mercado oficial
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
							<th className={styles.thPrice}>Cotización</th>
							<th className={styles.thAction}>Acción</th>
						</tr>
					</thead>
					<tbody>
						{items.map((player, index) => (
							<tr key={player.playerId} className={styles.row}>
								<td className={styles.tdRank}>
									<span
										className={`${styles.rankBadge} ${
											index === 0 ? styles.rankBadge_gold : ""
										}`}
									>
										{index === 0 ? "🏆 1" : `#${index + 1}`}
									</span>
								</td>

								<td className={styles.tdPlayer}>
									<div className={styles.playerMeta}>
										<div className={styles.avatarWrap}>
											<PlayerPortrait
												src={player.playerAvatar}
												name={player.playerName}
												size="sm"
												className={styles.avatar}
											/>
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
												{player.teamName ?? "Sin equipo"}
											</span>
										</div>
									</div>
								</td>

								<td className={styles.tdPos}>
									{player.tacticalPosition && (
										<PlayerPositionBadge position={player.tacticalPosition} />
									)}
								</td>

								<td className={styles.tdPrice}>
									<span className={styles.priceValue}>
										{(player.price ?? 0).toLocaleString("es-AR", {
											minimumFractionDigits: 2,
											maximumFractionDigits: 2,
										})}{" "}
										₵
									</span>
								</td>

								<td className={styles.tdAction}>
									<button
										type="button"
										className={styles.buyButton}
										onClick={(e) => handleBuy(player, e)}
										aria-label={`Comprar tokens de ${player.playerName}`}
									>
										<ShoppingCart size={12} aria-hidden="true" />
										Comprar
									</button>
								</td>
							</tr>
						))}
					</tbody>
				</table>
			</div>
		</section>
	);
}
