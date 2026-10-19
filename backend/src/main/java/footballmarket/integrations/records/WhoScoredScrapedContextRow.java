package footballmarket.integrations.records;

import java.time.Instant;
import java.util.List;

/** Fila de contexto externo extraída del DOM de una sección. */
public record WhoScoredScrapedContextRow(
    String section,
    String competitionId,
    String teamId,
    String seasonKey,
    String seasonLabel,
    String participationYears,
    List<WhoScoredScrapedMetricCell> cells,
    Instant observedAt) {
  public WhoScoredScrapedContextRow {
    cells = List.copyOf(cells);
  }
}
