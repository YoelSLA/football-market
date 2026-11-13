package footballmarket.features.portfolio.presentation;

import footballmarket.features.portfolio.domain.records.OperationHistoryEntry;
import footballmarket.features.portfolio.domain.records.OperationHistoryView;
import footballmarket.features.portfolio.presentation.dtos.response.OperationHistoryResponseDTO;
import footballmarket.features.portfolio.presentation.dtos.response.OperationHistoryResponseDTO.PaginationDTO;
import footballmarket.features.portfolio.presentation.dtos.response.OperationHistoryResponseDTO.PlayerDTO;
import footballmarket.features.portfolio.presentation.dtos.response.OperationHistoryResponseDTO.RelatedDTO;
import footballmarket.features.portfolio.presentation.dtos.response.OperationHistoryResponseDTO.SummaryDTO;
import footballmarket.features.portfolio.presentation.dtos.response.OperationHistoryResponseDTO.TransactionDTO;

/** Traducción del historial personal al contrato HTTP, sin reglas de negocio. */
public final class OperationHistoryMapper {
  private OperationHistoryMapper() {}

  /** Convierte la vista paginada completa. */
  public static OperationHistoryResponseDTO toResponse(OperationHistoryView view) {
    OperationHistoryView.Summary summary = view.summary();
    OperationHistoryView.Pagination pagination = view.pagination();
    return new OperationHistoryResponseDTO(
        new SummaryDTO(
            summary.totalTransactions(),
            summary.buyTransactions(),
            summary.sellTransactions(),
            summary.boughtTokens(),
            summary.soldTokens(),
            summary.purchasedVolume(),
            summary.soldVolume()),
        view.transactions().stream().map(OperationHistoryMapper::toResponse).toList(),
        new PaginationDTO(
            pagination.page(),
            pagination.size(),
            pagination.totalElements(),
            pagination.totalPages()));
  }

  private static TransactionDTO toResponse(OperationHistoryEntry entry) {
    OperationHistoryEntry.PlayerData player = entry.player();
    return new TransactionDTO(
        entry.transactionId(),
        entry.type(),
        new PlayerDTO(
            player.id(),
            player.name(),
            player.imageUrl(),
            player.fallbackImageUrl(),
            new RelatedDTO(player.team().id(), player.team().name()),
            new RelatedDTO(player.league().id(), player.league().name())),
        entry.quantity(),
        entry.unitPrice(),
        entry.totalAmount(),
        entry.executedAt());
  }
}
