package footballmarket.integrations.whoscored.records;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Respuesta completa de un jugador: una tabla por cada combinación conocida consultada. */
public record WhoScoredStatisticsResponse(
    String playerId, List<WhoScoredStatisticsTable> tables, Instant observedAt) {
  public WhoScoredStatisticsResponse {
    Objects.requireNonNull(playerId);
    Objects.requireNonNull(observedAt);
    tables = List.copyOf(tables);
  }
}
