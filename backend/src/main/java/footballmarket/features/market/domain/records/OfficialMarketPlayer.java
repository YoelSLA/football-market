package footballmarket.features.market.domain.records;

/**
 * Modelo inmutable de jugador del Mercado Oficial en la capa de dominio.
 *
 * @param id identificador único del jugador
 * @param name nombre completo del jugador
 * @param position posición deportiva normalizada
 * @param nationality país de nacionalidad
 * @param teamId identificador del club actual
 * @param teamName nombre del club actual
 * @param teamBadgeUrl URL del escudo del club
 * @param leagueId identificador de la liga
 * @param leagueName nombre de la liga
 * @param imageUrl URL de la fotografía del jugador
 * @param quote cotización vigente consolidada, o nulo si no registra cotización
 */
public record OfficialMarketPlayer(
    Long id,
    String name,
    String position,
    String nationality,
    Long teamId,
    String teamName,
    String teamBadgeUrl,
    Long leagueId,
    String leagueName,
    String imageUrl,
    OfficialMarketQuote quote) {}
