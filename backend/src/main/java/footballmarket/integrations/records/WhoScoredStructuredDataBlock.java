package footballmarket.integrations.records;

import java.math.BigDecimal;
import java.util.List;

/** Indicadores opcionales y contexto publicados en el HTML original del proveedor. */
public record WhoScoredStructuredDataBlock(List<Entry> entries) {
  public WhoScoredStructuredDataBlock {
    entries = List.copyOf(entries);
  }

  /** Cada entrada conserva los decimales originales y su contexto externo. */
  public record Entry(
      String competitionId,
      String seasonId,
      String teamId,
      BigDecimal rating,
      BigDecimal shotsOnTarget,
      BigDecimal secondYellow,
      BigDecimal gameStarted,
      BigDecimal subOn) {}
}
