import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useMemo } from "react";
import { type MarketPlayer, MarketSellPage } from "@/features/market";
import { type Player, useAllPlayersQuery } from "@/features/players";
import {
	portfolioQueryKeys,
	usePortfolioAllOpenHoldingsQuery,
} from "@/features/portfolio";
import { getErrorMessage } from "@/infrastructure/http";

function toMarketPlayer(player: Player): MarketPlayer {
	return {
		id: player.id,
		name: player.name,
		teamName: player.teamName,
		leagueName: player.leagueName,
		position: player.position,
		imageUrl: player.imageUrl,
		fallbackImageUrl: player.fallbackImageUrl,
	};
}

export default function MarketSellRoute() {
	const queryClient = useQueryClient();
	const playersQuery = useAllPlayersQuery();
	const holdingsQuery = usePortfolioAllOpenHoldingsQuery();
	const holdingsById = useMemo(
		() =>
			new Map(
				(holdingsQuery.data?.positions ?? []).map((position) => [
					position.playerId,
					position,
				]),
			),
		[holdingsQuery.data],
	);
	const players = (playersQuery.data?.players ?? []).flatMap((player) => {
		const holding = holdingsById.get(player.id);
		return holding
			? [
					{
						...toMarketPlayer(player),
						totalQuantity: holding.quantity,
						availableQuantity: holding.availableQuantity,
						reservedQuantity: holding.reservedQuantity,
					},
				]
			: [];
	});
	const refreshHoldings = useCallback(() => {
		const prefix = portfolioQueryKeys.holdings("OPEN", 0, 1).slice(0, 2);
		void queryClient.invalidateQueries({ queryKey: prefix });
	}, [queryClient]);
	const sellSuccess = useCallback(() => {
		void queryClient.invalidateQueries({
			queryKey: portfolioQueryKeys.summary,
		});
		refreshHoldings();
	}, [queryClient, refreshHoldings]);
	const loading =
		playersQuery.isPending ||
		playersQuery.isFetching ||
		holdingsQuery.isPending ||
		holdingsQuery.isFetching;
	const isError = playersQuery.isError || holdingsQuery.isError;
	const errorMessage = isError
		? getErrorMessage(
				playersQuery.error ?? holdingsQuery.error,
				"No se pudo cargar el mercado y las tenencias.",
			)
		: null;
	return (
		<MarketSellPage
			players={players}
			isLoadingPlayers={loading}
			isPlayersError={isError}
			errorMessage={errorMessage}
			onRetryPlayers={() => {
				void playersQuery.refetch();
				void holdingsQuery.refetch();
			}}
			onSellSuccess={sellSuccess}
			onRefreshHoldings={refreshHoldings}
		/>
	);
}
