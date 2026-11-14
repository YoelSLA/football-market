package footballmarket.features.quote.domain.records;

import footballmarket.features.quote.domain.exceptions.InvalidQuoteException;

/** Filtros conjuntos y página acotada de la consulta por cotización. */
public record QuoteRankingQuery(int page, int size, String name, Long leagueId, String position) {
  public QuoteRankingQuery {
    if (page < 0
        || size < 1
        || size > 100
        || (leagueId != null && leagueId <= 0)
        || (name != null && name.length() > 255)
        || (position != null && position.length() > 255)) {
      throw new InvalidQuoteException("Los parámetros del ranking de cotizaciones son inválidos");
    }
    name = normalize(name);
    position = normalize(position);
  }

  public long offset() {
    return (long) this.page * this.size;
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
