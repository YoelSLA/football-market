package footballmarket.models.records;

import footballmarket.models.enums.ValuationStrategy;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Ranking por score y contexto de la última ejecución de cotización.
 *
 * @param strategy estrategia de la última ejecución, o nula si no existen ejecuciones
 * @param quoteDate fecha de la última ejecución, o nula si no existen ejecuciones
 * @param entries entradas ordenadas del ranking, nunca nulas
 */
public record ScoreRanking(
    ValuationStrategy strategy, LocalDate quoteDate, List<ScoreRankingEntry> entries) {

  /** Conserva la lista como valor obligatorio del resultado. */
  public ScoreRanking {
    Objects.requireNonNull(entries, "entries");
  }
}
