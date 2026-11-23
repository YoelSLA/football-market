package footballmarket.features.market.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/**
 * Comprobante de una venta oficial confirmada.
 *
 * @param transactionId identificador del registro financiero
 * @param buyerId identificador del superusuario comprador
 * @param sellerId identificador del usuario vendedor
 * @param playerId identificador del jugador vendido
 * @param quantity cantidad de tokens transferida
 * @param quotePrice cotización de referencia aplicada en centésimas
 * @param discountPercentage comisión aplicada entre 10 y 99; se conserva el nombre del campo por
 *     compatibilidad
 * @param unitPrice precio neto por token en centésimas
 * @param totalAmount importe total transferido en centésimas
 * @param executedAt instante de confirmación
 */
public record OfficialBuybackResponseDTO(
    @Schema(description = "Identificador del registro financiero.", example = "12345")
        Long transactionId,
    @Schema(description = "Identificador del superusuario comprador.", example = "1") Long buyerId,
    @Schema(description = "Identificador del usuario vendedor.", example = "42") Long sellerId,
    @Schema(description = "Identificador del jugador vendido.", example = "7821") Long playerId,
    @Schema(description = "Cantidad de tokens vendidos.", example = "5") int quantity,
    @Schema(description = "Cotización de referencia en centésimas.", example = "100")
        long quotePrice,
    @Schema(
            description =
                "Comisión porcentual aplicada a la cotización, entre 10 y 99 inclusive. Se conserva"
                    + " el nombre discountPercentage por compatibilidad.",
            minimum = "10",
            maximum = "99",
            example = "10")
        int discountPercentage,
    @Schema(description = "Precio neto por token en centésimas.", example = "90") long unitPrice,
    @Schema(description = "Importe total transferido en centésimas.", example = "450")
        long totalAmount,
    @Schema(
            description = "Instante de confirmación de la operación.",
            example = "2026-11-22T10:30:00Z")
        OffsetDateTime executedAt) {}
