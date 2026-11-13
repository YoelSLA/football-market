package footballmarket.features.market.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representación de un jugador en el catálogo del Mercado Oficial con cotización integrada.
 *
 * @param id identificador único del jugador
 * @param name nombre completo del jugador
 * @param position posición deportiva normalizada
 * @param nationality país de nacionalidad
 * @param teamId identificador del club o equipo deportivo actual
 * @param teamName nombre del club deportivo actual
 * @param teamBadgeUrl URL del escudo oficial del club
 * @param leagueId identificador de la liga deportiva
 * @param leagueName nombre de la liga deportiva
 * @param imageUrl URL de la fotografía del jugador
 * @param quote cotización vigente consolidada o nulo si no registra cotización
 */
@Schema(
    name = "OfficialMarketPlayerResponse",
    description = "Jugador elegible en el Mercado Oficial con su cotización vigente integrada")
public record OfficialMarketPlayerResponseDTO(
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
    @Schema(
            description =
                "Cotización vigente consolidada del jugador. Nulo si no registra cotización en el sistema",
            nullable = true)
        OfficialMarketQuoteResponseDTO quote) {}
