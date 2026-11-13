package footballmarket.features.market.application;

/**
 * Value object inmutable con los criterios normalizados para consultar el catálogo del Mercado
 * Oficial.
 *
 * @param page índice de página (base 0)
 * @param size tamaño de página
 * @param sortProperty propiedad de ordenamiento principal ('id', 'name', 'price', 'variation')
 * @param sortDirection dirección de ordenamiento ('asc', 'desc')
 * @param name filtro por nombre recortado, o nulo si no se especifica
 * @param leagueId identificador de liga, o nulo
 * @param teamId identificador de equipo, o nulo
 * @param position posición deportiva, o nulo
 * @param nationality país de nacionalidad, o nulo
 * @param hasQuote filtro de disponibilidad de cotización, o nulo
 * @param minPrice precio mínimo en centésimas, o nulo
 * @param maxPrice precio máximo en centésimas, o nulo
 */
public record OfficialMarketQuery(
    int page,
    int size,
    String sortProperty,
    String sortDirection,
    String name,
    Long leagueId,
    Long teamId,
    String position,
    String nationality,
    Boolean hasQuote,
    Long minPrice,
    Long maxPrice) {

  /**
   * Determina si la consulta involucra filtros financieros que requieran evaluar la cotización en
   * la consulta de conteo.
   *
   * @return {@code true} si se filtra por minPrice, maxPrice o hasQuote
   */
  public boolean hasFinancialFilters() {
    return this.minPrice != null || this.maxPrice != null || this.hasQuote != null;
  }
}
