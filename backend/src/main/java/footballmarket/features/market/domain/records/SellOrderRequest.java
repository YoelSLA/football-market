package footballmarket.features.market.domain.records;

/** Intención de publicar tokens; el vendedor siempre procede de la sesión. */
public record SellOrderRequest(Long playerId, int quantity, Long unitPrice) {
  /** Mantiene la construcción de solicitudes sin precio explícito. */
  public SellOrderRequest(Long playerId, int quantity) {
    this(playerId, quantity, null);
  }
}
