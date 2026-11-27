package footballmarket.features.quote.domain.records;

import footballmarket.features.quote.domain.exceptions.InvalidQuoteException;

/** Filtros conjuntos y página acotada de la consulta por cotización. */
public record QuoteRankingQuery(
    int page, int size, String name, Long leagueId, String position, String order) {
  public QuoteRankingQuery {
    if (page < 0
        || size < 1
        || size > 100
        || (leagueId != null && leagueId <= 0)
        || (name != null && name.length() > 255)
        || (position != null && position.length() > 255)
        || (order != null && !order.equalsIgnoreCase("asc") && !order.equalsIgnoreCase("desc"))) {
      throw new InvalidQuoteException("Los parámetros del ranking de cotizaciones son inválidos");
    }
    name = normalize(name);
    position = normalize(position);
    order = order == null || order.isBlank() ? "desc" : order.toLowerCase(java.util.Locale.ROOT);
  }

  public QuoteRankingQuery(int page, int size, String name, Long leagueId, String position) {
    this(page, size, name, leagueId, position, "desc");
  }

  public long offset() {
    return (long) this.page * this.size;
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
