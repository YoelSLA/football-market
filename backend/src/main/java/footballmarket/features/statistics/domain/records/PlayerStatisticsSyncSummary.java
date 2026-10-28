package footballmarket.features.statistics.domain.records;

import footballmarket.features.statistics.domain.enums.ExtractionRunStatus;
import footballmarket.features.statistics.domain.enums.MetricSource;
import java.time.Instant;
import java.time.LocalDate;

public record PlayerStatisticsSyncSummary(
    Long runId,
    MetricSource source,
    LocalDate extractionDate,
    ExtractionRunStatus status,
    Instant startedAt,
    Instant finishedAt,
    PlayerStatisticsSyncCounters counters,
    long providerCalls,
    boolean coverageComplete) {}
