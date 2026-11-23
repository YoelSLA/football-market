package footballmarket.features.market.domain.records;

/**
 * Solicitud de venta oficial expresada en el modelo de dominio.
 *
 * @param playerId identificador interno del jugador que se vende
 * @param quantity cantidad de tokens a vender
 */
public record OfficialBuybackRequest(Long playerId, int quantity) {}
