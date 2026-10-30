package footballmarket.features.portfolio.presentation;

import footballmarket.features.portfolio.domain.records.PortfolioView;
import footballmarket.features.portfolio.presentation.dtos.response.PortfolioPaginationResponseDTO;
import footballmarket.features.portfolio.presentation.dtos.response.PortfolioPositionResponseDTO;
import footballmarket.features.portfolio.presentation.dtos.response.PortfolioResponseDTO;
import footballmarket.features.portfolio.presentation.dtos.response.PortfolioSummaryResponseDTO;

/** Traducción de la vista de dominio del portfolio a los DTOs HTTP, sin cálculos ni reglas. */
public final class PortfolioMapper {
  private PortfolioMapper() {}

  /** Convierte la vista completa del portfolio en la respuesta HTTP. */
  public static PortfolioResponseDTO toResponse(PortfolioView view) {
    return new PortfolioResponseDTO(
        toSummary(view.summary()),
        view.positions().stream().map(PortfolioMapper::toPosition).toList(),
        toPagination(view.pagination()));
  }

  private static PortfolioSummaryResponseDTO toSummary(PortfolioView.PortfolioSummary summary) {
    return new PortfolioSummaryResponseDTO(
        summary.totalPositions(),
        summary.totalTokens(),
        summary.totalReservedTokens(),
        summary.totalAvailableTokens(),
        summary.totalInvested(),
        summary.totalCurrentValue(),
        summary.totalRealizedProfitLoss(),
        summary.totalUnrealizedProfitLoss(),
        summary.totalProfitLoss(),
        summary.totalProfitLossPercentage(),
        summary.totalBuyTransactions(),
        summary.totalSellTransactions(),
        summary.valuationComplete());
  }

  private static PortfolioPositionResponseDTO toPosition(PortfolioView.PortfolioPosition position) {
    return new PortfolioPositionResponseDTO(
        position.playerId(),
        position.playerName(),
        position.teamName(),
        position.position(),
        position.imageUrl(),
        position.active(),
        position.status(),
        position.valuationStatus(),
        position.quantity(),
        position.reservedQuantity(),
        position.availableQuantity(),
        position.acquisitionCost(),
        position.averageUnitPrice(),
        position.currentUnitPrice(),
        position.priceUpdatedAt(),
        position.currentValue(),
        position.realizedProfitLoss(),
        position.unrealizedProfitLoss(),
        position.totalProfitLoss(),
        position.unrealizedProfitLossPercentage(),
        position.portfolioWeightPercentage(),
        position.openedAt(),
        position.closedAt(),
        position.lastTransactionAt(),
        position.totalBuyTransactions(),
        position.totalSellTransactions());
  }

  private static PortfolioPaginationResponseDTO toPagination(
      PortfolioView.PortfolioPagination pagination) {
    return new PortfolioPaginationResponseDTO(
        pagination.page(), pagination.size(), pagination.totalElements(), pagination.totalPages());
  }
}
