package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/**
 * Resultado de una compra confirmada.
 *
 * @param transactionId identificador del registro financiero de la operación
 * @param playerId identificador interno del jugador comprado
 * @param quantity cantidad de tokens transferida al comprador
 * @param unitPrice precio por token aplicado, en centésimas de crédito
 * @param totalAmount importe total de la operación, en centésimas de crédito
 * @param executedAt instante de confirmación de la operación
 */
public record OrderBuyResponseDTO(
    @Schema(description = "Identificador del registro financiero de la compra.", example = "12345")
        Long transactionId,
    @Schema(description = "Identificador interno del jugador comprado.", example = "7821")
        Long playerId,
    @Schema(description = "Cantidad de tokens comprados.", example = "10") Integer quantity,
    @Schema(
            description =
                "Precio unitario por token en centésimas de crédito. 100 equivale a 1"
                    + " crédito.",
            example = "100")
        Long unitPrice,
    @Schema(description = "Importe total de la compra en centésimas de crédito.", example = "1000")
        Long totalAmount,
    @Schema(
            description = "Instante de confirmación de la operación.",
            example = "2026-10-14T10:30:00.123Z")
        OffsetDateTime executedAt) {}
