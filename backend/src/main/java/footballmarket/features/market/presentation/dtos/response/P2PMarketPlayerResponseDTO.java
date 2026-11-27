package footballmarket.features.market.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representación de un jugador en el catálogo del Mercado P2P con datos agregados de publicaciones.
 */
@Schema(
    name = "P2PMarketPlayerResponse",
    description = "Jugador en el Mercado P2P con métricas consolidadas de ofertas activas")
public record P2PMarketPlayerResponseDTO(
    @Schema(description = "Identificador único del jugador", example = "7821") Long id,
    @Schema(description = "Nombre completo del jugador", example = "Lionel Messi") String name,
    @Schema(description = "Posición deportiva normalizada", example = "DEL") String position,
    @Schema(description = "País de nacionalidad", example = "Argentina", nullable = true)
        String nationality,
    @Schema(description = "Identificador del club actual", example = "45", nullable = true)
        Long teamId,
    @Schema(description = "Nombre del club actual", example = "Inter Miami CF", nullable = true)
        String teamName,
    @Schema(
            description = "URL del escudo oficial del club",
            example = "https://images.footballmarket.com/teams/45.png",
            nullable = true)
        String teamBadgeUrl,
    @Schema(description = "Identificador de la liga", example = "12", nullable = true)
        Long leagueId,
    @Schema(description = "Nombre de la liga", example = "Major League Soccer", nullable = true)
        String leagueName,
    @Schema(
            description = "URL de la fotografía del jugador",
            example = "https://images.footballmarket.com/players/7821.png",
            nullable = true)
        String imageUrl,
    @Schema(description = "Cantidad de publicaciones activas para el jugador", example = "3")
        long listingCount,
    @Schema(description = "Cantidad total de tokens disponibles en la comunidad", example = "25")
        long availableQuantity,
    @Schema(
            description = "Menor precio unitario disponible en centésimas de crédito",
            example = "1500")
        long minimumUnitPrice) {}
