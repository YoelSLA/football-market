package footballmarket.integrations.whoscored.records;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Fila de contexto del canal JSON con las celdas publicadas de su combinación. */
public record WhoScoredStatisticsTable(
    String section,
    String competitionId,
    String teamId,
    String seasonKey,
    String seasonLabel,
    String participationYears,
    List<Cell> cells,
    Instant observedAt) {
  public WhoScoredStatisticsTable {
    Objects.requireNonNull(section);
    Objects.requireNonNull(observedAt);
    cells = List.copyOf(cells);
  }

  /**
   * Celda publicada por el proveedor.
   *
   * <p>{@code text} conserva el token original para que la normalización lea el decimal exacto sin
   * conversión binaria; nunca contiene HTML ni cabeceras.
   */
  public record Cell(String providerField, String text) {}
}
