import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useState } from "react";
import { MarketBuyPage, type MarketPlayer } from "@/features/market";
import { type Player, usePlayersQuery } from "@/features/players";
import { portfolioQueryKeys } from "@/features/portfolio";
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

/**
 * Adaptador de composición para la ruta /market/buy (SPEC-019).
 * La capa app compone features aisladas:
 * - Consulta catálogo de players con usePlayersQuery(page)
 * - Convierte Player a MarketPlayer sin filtrar por stock
 * - Pasa estado paginado a MarketBuyPage
 * - Invalida summary y holdings de Portfolio tras compra confirmada
 */
export default function MarketBuyRoute() {
	const [currentPage, setCurrentPage] = useState(0);
	const queryClient = useQueryClient();

	const playersQuery = usePlayersQuery(currentPage);

	const isLoading = playersQuery.isPending || playersQuery.isFetching;
	const isError = playersQuery.isError;
	const errorMessage = isError
		? getErrorMessage(
				playersQuery.error,
				"No se pudo cargar el listado de jugadores del mercado.",
			)
		: null;

	const players: MarketPlayer[] =
		playersQuery.data?.players.map(toMarketPlayer) ?? [];
	const totalPages = playersQuery.data?.totalPages ?? 0;

	const handlePurchaseSuccess = useCallback(
		(_playerId: number) => {
			// Invalida summary de Portfolio
			queryClient.invalidateQueries({
				queryKey: portfolioQueryKeys.summary,
			});

			// Invalida todas las holdings usando el prefijo derivado de portfolioQueryKeys.holdings
			const holdingsPrefix = portfolioQueryKeys
				.holdings("ALL", 0, 1)
				.slice(0, 2);
			queryClient.invalidateQueries({
				queryKey: holdingsPrefix,
			});
		},
		[queryClient],
	);

	return (
		<MarketBuyPage
			players={players}
			isLoadingPlayers={isLoading}
			isPlayersError={isError}
			errorMessage={errorMessage}
			onRetryPlayers={playersQuery.refetch}
			currentPage={currentPage}
			totalPlayers={playersQuery.data?.totalElements}
			pageSize={playersQuery.data?.size}
			totalPages={totalPages}
			onPageChange={setCurrentPage}
			onPurchaseSuccess={handlePurchaseSuccess}
		/>
	);
}
