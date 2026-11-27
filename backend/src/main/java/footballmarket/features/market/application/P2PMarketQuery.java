package footballmarket.features.market.application;

/**
 * Criterios normalizados para consultar el catálogo del Mercado P2P.
 *
 * @param page índice de página (base 0)
 * @param size tamaño de página
 * @param sortProperty propiedad de ordenamiento ('price', 'quantity', 'name', 'id')
 * @param sortDirection dirección ('asc', 'desc')
 * @param search texto de búsqueda por nombre de jugador o club
 * @param leagueId identificador de liga, o nulo
 * @param teamId identificador de club, o nulo
 * @param position posición deportiva, o nulo
 * @param minPrice precio unitario mínimo en centésimas, o nulo
 * @param maxPrice precio unitario máximo en centésimas, o nulo
 */
public record P2PMarketQuery(
    int page,
    int size,
    String sortProperty,
    String sortDirection,
    String search,
    Long leagueId,
    Long teamId,
    String position,
    Long minPrice,
    Long maxPrice) {}
