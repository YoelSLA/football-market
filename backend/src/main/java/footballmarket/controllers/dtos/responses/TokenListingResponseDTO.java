package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** Detalle de publicación con estado efectivo y cierre explícito nullable. */
public record TokenListingResponseDTO(
    @Schema(description = "Identificador de publicación.") Long listingId,
    @Schema(description = "Identificador del vendedor.") Long sellerId,
    @Schema(description = "Identificador del jugador.") Long playerId,
    @Schema(description = "Cantidad completa publicada.") int quantity,
    @Schema(description = "Precio unitario congelado en centésimas.") long unitPrice,
    @Schema(description = "Importe congelado en centésimas.") long totalAmount,
    @Schema(
            description = "Estado efectivo.",
            allowableValues = {"ACTIVE", "SOLD", "CANCELLED", "EXPIRED"})
        String status,
    @Schema(description = "Instante ISO-8601 con offset; creación.") OffsetDateTime createdAt,
    @Schema(description = "Instante ISO-8601 con offset; vencimiento.") OffsetDateTime expiresAt,
    @Schema(
            description =
                "Instante ISO-8601 con offset; cierre por venta o cancelación. Null incluso al expirar sin acción explícita.",
            nullable = true)
        OffsetDateTime closedAt) {}
