package footballmarket.features.market.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** Elemento de listado, sin el campo closedAt que pertenece al detalle contractual. */
public record TokenListingSummaryResponseDTO(
    @Schema(description = "Identificador de publicación.") Long listingId,
    @Schema(description = "Identificador del vendedor.") Long sellerId,
    @Schema(description = "Identificador del jugador.") Long playerId,
    @Schema(description = "Cantidad completa publicada.") int quantity,
    @Schema(description = "Precio unitario en centésimas.") long unitPrice,
    @Schema(description = "Importe total en centésimas.") long totalAmount,
    @Schema(
            description = "Estado efectivo.",
            allowableValues = {"ACTIVE", "SOLD", "CANCELLED", "EXPIRED"})
        String status,
    @Schema(description = "Instante ISO-8601 con offset; creación.") OffsetDateTime createdAt,
    @Schema(description = "Instante ISO-8601 con offset; vencimiento.") OffsetDateTime expiresAt,
    @Schema(description = "Nombre del jugador.") String playerName,
    @Schema(description = "ID del club.") Long teamId,
    @Schema(description = "Nombre del club.") String teamName,
    @Schema(description = "ID de la liga.") Long leagueId,
    @Schema(description = "Nombre de la liga.") String leagueName,
    @Schema(description = "Posición del jugador.") String position,
    @Schema(description = "URL de imagen del jugador.") String imageUrl,
    @Schema(description = "URL de imagen de respaldo.") String fallbackImageUrl) {}
