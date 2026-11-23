package footballmarket.features.market.domain.records;

import java.time.OffsetDateTime;

/**
 * Comprobante de una venta oficial confirmada.
 *
 * <p>Identifica a las partes, el jugador, la cantidad, la cotización de referencia, la comisión
 * aplicada, el precio neto por token y el importe transferido, en centésimas de crédito.
 *
 * @param transactionId identificador del registro financiero
 * @param buyerId identificador del superusuario comprador
 * @param sellerId identificador del usuario vendedor
 * @param playerId identificador del jugador vendido
 * @param quantity cantidad de tokens transferida
 * @param quotePrice cotización de referencia aplicada
 * @param discountPercentage comisión aplicada entre 10 y 99; se conserva el nombre por
 *     compatibilidad
 * @param unitPrice precio neto por token
 * @param totalAmount importe total transferido
 * @param executedAt instante de confirmación
 */
public record OfficialBuybackReceipt(
    Long transactionId,
    Long buyerId,
    Long sellerId,
    Long playerId,
    int quantity,
    long quotePrice,
    int discountPercentage,
    long unitPrice,
    long totalAmount,
    OffsetDateTime executedAt) {}
