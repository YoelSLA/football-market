package footballmarket.features.ranking.application;

import footballmarket.features.ranking.domain.records.ScoreRanking;
import footballmarket.features.ranking.domain.records.ScoreRankingPage;
import footballmarket.features.ranking.domain.records.ScoreRankingQuery;

/** Consulta de solo lectura del ranking por score de la última estrategia cotizada. */
public interface ScoreRankingQueryService {

  /**
   * Obtiene el ranking sin recalcular ni modificar cotizaciones o valuaciones.
   *
   * @return contexto de la última ejecución y entradas ordenadas; nunca {@code null}
   */
  ScoreRanking findScoreRanking();

  /** Consulta una página filtrada conservando las posiciones globales. */
  ScoreRankingPage findScoreRanking(ScoreRankingQuery query);
}
