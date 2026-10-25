package footballmarket.models.records;

import java.time.OffsetDateTime;

/** Vista inmutable de una transacción desde la perspectiva del usuario autenticado. */
public record OperationHistoryEntry(
    Long transactionId,
    OperationType type,
    Long playerId,
    String playerName,
    int quantity,
    long unitPrice,
    long totalAmount,
    OffsetDateTime executedAt) {

  /** Rol del usuario autenticado en la transacción. */
  public enum OperationType {
    BUY,
    SELL
  }
}
