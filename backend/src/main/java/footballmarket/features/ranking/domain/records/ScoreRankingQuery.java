package footballmarket.features.ranking.domain.records;

import footballmarket.features.quote.domain.records.QuoteRankingQuery;

/** Página y filtros de la consulta del ranking por score. */
public record ScoreRankingQuery(int page, int size, String name, Long leagueId, String position) {
  public ScoreRankingQuery {
    QuoteRankingQuery query = new QuoteRankingQuery(page, size, name, leagueId, position);
    name = query.name();
    position = query.position();
  }

  public long offset() {
    return (long) this.page * this.size;
  }
}
