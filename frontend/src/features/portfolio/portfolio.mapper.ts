import type { Portfolio, PortfolioResponseDTO } from "./types";

/** Conserva el contrato sin revalidar ni recalcular sus garantías financieras. */
export function toPortfolio(dto: PortfolioResponseDTO): Portfolio {
	return {
		summary: { ...dto.summary },
		positions: dto.positions.map((position) => ({ ...position })),
		pagination: { ...dto.pagination },
	};
}
