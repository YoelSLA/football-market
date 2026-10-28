package footballmarket.features.market.domain.records;

/**
 * Solicitud de compra expresada en el modelo de dominio.
 *
 * <p>Transporta la intención de compra desde la frontera HTTP hasta el caso de uso sin que la capa
 * HTTP dependa del repositorio: la resolución del jugador y de su elegibilidad pertenecen al
 * servicio, que es quien puede distinguir inexistencia de falta de visibilidad.
 *
 * @param playerId identificador interno del jugador que se quiere comprar
 * @param quantity cantidad de tokens a comprar
 */
public record BuyOrderRequest(Long playerId, int quantity) {}
