package footballmarket.features.ranking.domain.records;

import java.util.List;

/** Opciones globales del universo elegible, independientes de los filtros de la página. */
public record ScoreRankingFilterOptions(List<League> leagues, List<String> positions) {
  /** Liga presente en el universo elegible. */
  public record League(Long id, String name) {}
}
