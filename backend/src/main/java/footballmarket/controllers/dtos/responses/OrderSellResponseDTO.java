package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** Publicación recién creada; importes en centésimas, sin transferencia de recursos. */
public record OrderSellResponseDTO(
    @Schema(description = "Identificador de la publicación.") Long listingId,
    @Schema(description = "Jugador publicado.") Long playerId,
    @Schema(description = "Tokens reservados.") int quantity,
    @Schema(description = "Precio congelado por token, en centésimas.") long unitPrice,
    @Schema(description = "Importe total congelado, en centésimas.") long totalAmount,
    @Schema(description = "Estado inicial ACTIVE.") String status,
    @Schema(description = "Instante ISO-8601 con offset; creación.") OffsetDateTime createdAt,
    @Schema(
            description =
                "Instante ISO-8601 con offset; vencimiento, por defecto 24 horas después.")
        OffsetDateTime expiresAt) {}
