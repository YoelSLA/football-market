package footballmarket.features.market.domain.records;

/** Resumen de volumen de compra de un jugador durante la ventana consultada. */
public record MarketHotPlayer(
    Long playerId, String playerName, long boughtTokens, long buyVolume) {}
