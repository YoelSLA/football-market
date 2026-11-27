import { useMemo } from "react";
import { useCurrentUserQuery } from "@/features/auth";
import { usePortfolioAllOpenHoldingsQuery } from "@/features/portfolio/hooks/queries";
import type { PortfolioPosition } from "@/features/portfolio/types";
import { AuthStatus, useAuthStore } from "@/infrastructure/storage";

export interface PlayerHoldingResult {
	holding: PortfolioPosition | null;
	availableCredits?: number;
	availableCreditsCentimes?: number;
	isLoading: boolean;
}

/**
 * Consulta la posición abierta del usuario autenticado en la cartera para un jugador específico.
 */
export function usePlayerHolding(
	playerId: number | null | undefined,
): PlayerHoldingResult {
	const revision = useAuthStore((state) => state.revision);
	const authenticated = useAuthStore(
		(state) => state.status === AuthStatus.AUTHENTICATED,
	);
	const currentUser = useCurrentUserQuery(authenticated, revision);
	const holdingsQuery = usePortfolioAllOpenHoldingsQuery();

	const holding = useMemo(() => {
		if (!playerId || !holdingsQuery.data?.positions) return null;
		return (
			holdingsQuery.data.positions.find(
				(pos: PortfolioPosition) => pos.playerId === playerId,
			) ?? null
		);
	}, [playerId, holdingsQuery.data?.positions]);

	return {
		holding,
		availableCredits: currentUser.data?.availableCredits,
		availableCreditsCentimes: currentUser.data?.availableCreditsCentimes,
		isLoading: holdingsQuery.isPending,
	};
}
