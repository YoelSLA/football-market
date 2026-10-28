package footballmarket.features.portfolio.presentation;

import footballmarket.features.portfolio.domain.records.OperationHistoryEntry;
import footballmarket.features.portfolio.presentation.dtos.response.OperationHistoryResponseDTO;
import java.util.List;

/** Traducción de la vista del historial personal al contrato HTTP, sin reglas de negocio. */
public final class OperationHistoryMapper {
  private OperationHistoryMapper() {}

  /** Convierte todas las entradas conservando su orden. */
  public static List<OperationHistoryResponseDTO> toResponse(List<OperationHistoryEntry> entries) {
    return entries.stream().map(OperationHistoryMapper::toResponse).toList();
  }

  private static OperationHistoryResponseDTO toResponse(OperationHistoryEntry entry) {
    return new OperationHistoryResponseDTO(
        entry.transactionId(),
        entry.type(),
        entry.playerId(),
        entry.playerName(),
        entry.quantity(),
        entry.unitPrice(),
        entry.totalAmount(),
        entry.executedAt());
  }
}
