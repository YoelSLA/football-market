import type {
	OperationHistoryResponseDTO,
	Portfolio,
	PortfolioResponseDTO,
	TransactionHistory,
} from "./types";
import { TransactionType } from "./types";

/** Conserva el contrato sin revalidar ni recalcular sus garantías financieras. */
export function toPortfolio(dto: PortfolioResponseDTO): Portfolio {
	return {
		summary: { ...dto.summary },
		positions: dto.positions.map((position) => ({ ...position })),
		pagination: { ...dto.pagination },
	};
}

/** Conserva los valores históricos y el orden entregado por el backend. */
export function toTransactionHistory(
	dto: OperationHistoryResponseDTO,
): TransactionHistory {
	return {
		summary: { ...dto.summary },
		transactions: dto.transactions.map((entry) => ({
			...entry,
			player: {
				...entry.player,
				team: { ...entry.player.team },
				league: { ...entry.player.league },
			},
			type: TransactionType[entry.type],
		})),
		pagination: { ...dto.pagination },
	};
}
