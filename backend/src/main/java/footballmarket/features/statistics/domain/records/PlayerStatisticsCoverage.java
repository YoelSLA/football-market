package footballmarket.features.statistics.domain.records;

import footballmarket.features.statistics.domain.enums.MetricSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PlayerStatisticsCoverage(
    MetricSource source,
    LocalDate extractionDate,
    long catalogPlayers,
    long eligiblePlayers,
    long excludedWithoutReference,
    long completedPlayers,
    long pendingPlayers,
    long failedPlayers,
    boolean coverageComplete,
    List<String> systemicallyAbsentMetrics,
    Long lastRunId,
    Instant lastRunFinishedAt) {
  public PlayerStatisticsCoverage {
    systemicallyAbsentMetrics = List.copyOf(systemicallyAbsentMetrics);
  }
}
