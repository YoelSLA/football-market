package footballmarket.features.portfolio.domain.records;

import java.time.OffsetDateTime;

/** Vista inmutable de una transacción desde la perspectiva del usuario autenticado. */
public record OperationHistoryEntry(
    Long transactionId,
    OperationType type,
    PlayerData player,
    int quantity,
    long unitPrice,
    long totalAmount,
    OffsetDateTime executedAt) {

  /** Rol del usuario autenticado en la transacción. */
  public enum OperationType {
    BUY,
    SELL
  }

  /** Perfil actual del jugador asociado, no una fotografía histórica. */
  public record PlayerData(
      Long id,
      String name,
      String imageUrl,
      String fallbackImageUrl,
      RelatedData team,
      RelatedData league) {}

  /** Identidad y nombre actuales de una relación del catálogo. */
  public record RelatedData(Long id, String name) {}
}
