package footballmarket.features.catalog.domain.records;

/** Criterios de lectura; la normalización de acentos se aplica únicamente en PostgreSQL. */
public record PlayerCatalogCriteria(
    String name,
    Long leagueId,
    Long teamId,
    String position,
    String nationality,
    String sort,
    boolean includeStatistics) {
  public PlayerCatalogCriteria {
    name = clean(name);
    position = clean(position);
    nationality = clean(nationality);
    sort = sort == null ? "id,asc" : sort;
  }

  private static String clean(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
